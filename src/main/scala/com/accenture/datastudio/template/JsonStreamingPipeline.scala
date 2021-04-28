package com.accenture.datastudio.template

import com.accenture.datastudio.core.io.Sink
import org.apache.beam.sdk.values.{KV, PCollection, TupleTag, TypeDescriptors}
import org.slf4j.{Logger, LoggerFactory}
import org.joda.time.Duration
import com.accenture.datastudio.core.templates._
import com.google.api.services.bigquery.model.TableRow
import com.spotify.scio.bigquery.types.BigQueryType
import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.io.gcp.pubsub.{PubsubIO, PubsubMessage}
import org.apache.beam.sdk.options.ValueProvider
import org.apache.beam.sdk.transforms.DoFn.{ProcessElement, Setup}
import org.apache.beam.sdk.transforms.windowing.{FixedWindows, Window}
import org.apache.beam.sdk.transforms.{DoFn, MapElements, ParDo}
import org.apache.beam.sdk.values._
import com.accenture.datastudio.core.io.SinkInstance._
import com.accenture.datastudio.model.Appointment.Appointments
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import org.json4s.jackson.JsonMethods.parse
import com.google.gson.Gson

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

object JsonStreamingPipeline {

  import com.accenture.datastudio.core.valueproviders.ValueProviderConfigExtProvider._
  import com.accenture.datastudio.core.valueproviders.ValueProviderConfigExtSyntax._

  //val jsonKVSuccessTag: TupleTag[KV[Appointments, java.util.Map[String, String]]] = new TupleTag[KV[Appointments, java.util.Map[String, String]]]() {}
  val jsonKVSuccessTag: TupleTag[Appointments] = new TupleTag[Appointments]() {}
  val LOG: Logger = LoggerFactory.getLogger(this.getClass)

  private val extractPubSubMessage = () => new DoFn[PubsubMessage, KV[String, java.util.Map[String, String]]] {
    @ProcessElement
    def processElement(c: ProcessContext): Unit = {
      val payload = c.element().getPayload
      c.output(KV.of(new String(payload), c.element().getAttributeMap))
    }
  }

  //private val parseMessageBody = () => new DoFn[KV[String, java.util.Map[String, String]], KV[Appointments, java.util.Map[String, String]]]() {
  private val parseMessageBody = () => new DoFn[KV[String, java.util.Map[String, String]], Appointments]() {
    //@transient val gson = new Gson

    @ProcessElement
    def processElement(c: ProcessContext): Unit = {

      val jsonData = c.element().getKey
      val attributes = c.element().getValue

      val objectMapper = new ObjectMapper() with ScalaObjectMapper
      objectMapper.registerModule(DefaultScalaModule)

      implicit val formats = org.json4s.DefaultFormats
      val messageMap = parse(jsonData).extract[Map[String, Any]]
      val finalMap = messageMap.++(attributes.asScala)
      val jsonFull = objectMapper.writeValueAsString(finalMap)

      Try(Appointments(jsonFull)) match {
        case Success(appointments) => c.output(jsonKVSuccessTag, appointments)
        case Failure(exception) =>
          LOG.error(s"Failed Transform. ${exception.getMessage}")
          c.output(stringFailedTag, jsonData)
      }

      //c.output(jsonKVSuccessTag,KV.of(appointments,c.element().getValue))
    }
  }

  def build(subscription: ValueProvider[String])(implicit p: Pipeline, config: ValueProvider[String]): Unit = {
    implicit val root = None
    val (successPCollection, failurePCollection) = transforms(p.apply("pub/sub-read", PubsubIO.readMessagesWithAttributes().fromSubscription(subscription)))
    successPCollection.apply(Sink.bigquerySinkDynamicStream("BigQuerySinkDynamicStream"))

    failurePCollection.apply(Window.into(FixedWindows.of(Duration.standardMinutes(1))))
      .apply("invalid-records", writeToFile(errorPath.toValueProvider))

  }

  def transforms(messages: PCollection[PubsubMessage])(implicit config: ValueProvider[String]) = {

    implicit val root = None

    messages.apply(MapElements
      .into(TypeDescriptors.strings())
      .via((msg: PubsubMessage) => {
        val xml = msg.getPayload.map(_.toChar).mkString
        val attributes = msg.getAttributeMap.asScala.toMap
        s"$xml,${metadata(attributes)}"
      }))
      .apply(Window.into(FixedWindows.of(Duration.standardMinutes(1))))
      .apply(writeToFile(rawBucketPath.toValueProvider))

    val parsed = messages
      .apply("extract-message", ParDo.of(extractPubSubMessage()))
      .apply("process-message", ParDo.of(parseMessageBody()).withOutputTags(jsonKVSuccessTag, TupleTagList.of(stringFailedTag)))
    val success = parsed.get(jsonKVSuccessTag).apply("to-table-row", MapElements.into(TypeDescriptor.of(classOf[TableRow]))
      .via((appointments: Appointments) => {
        val bqt = BigQueryType[Appointments]
        bqt.toTableRow(appointments)
      }))

    val failure = parsed.get(stringFailedTag)
    (success, failure)
  }
}
