package com.accenture.datastudio.template

import java.io.ByteArrayInputStream
import java.util.Random

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.api.services.bigquery.model.TableRow
import com.accenture.datastudio.core.io.Sink
import com.accenture.datastudio.core.io.SinkInstance._
import com.accenture.datastudio.core.templates._
import domain.datapump.xml.model.{Appointments,Data}
import com.accenture.datastudio.transformers.helpers.TableRowSyntax._
import com.accenture.datastudio.transformers.helpers.TableRowWriterInstance._
import javax.xml.bind.{JAXBContext, Unmarshaller}
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.util.StreamReaderDelegate
import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.io.gcp.pubsub.{PubsubIO, PubsubMessage}
import org.apache.beam.sdk.options.ValueProvider
import org.apache.beam.sdk.transforms.DoFn.{ProcessElement, Setup}
import org.apache.beam.sdk.transforms.windowing.{FixedWindows, Window}
import org.apache.beam.sdk.transforms.{DoFn, GroupIntoBatches, MapElements, ParDo}
import org.apache.beam.sdk.values._
import org.joda.time.Duration
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

object XmlStreamingPipeline {

  import com.accenture.datastudio.core.valueproviders.ValueProviderConfigExtProvider._
  import com.accenture.datastudio.core.valueproviders.ValueProviderConfigExtSyntax._

  val apptKVSuccessTag: TupleTag[KV[Appointments, java.util.Map[String,String]]] = new TupleTag[KV[Appointments, java.util.Map[String,String]]]() {}

  val LOG: Logger = LoggerFactory.getLogger(this.getClass)

  lazy val mapper = new ObjectMapper()

  val toApptFn = () => new DoFn[KV[java.lang.Integer,java.lang.Iterable[PubsubMessage]], KV[Appointments,java.util.Map[String,String]]]() {
    var unmarshaller:Unmarshaller = _
    var xmlInputFactory:XMLInputFactory = _

    @Setup
    def init(): Unit ={
      unmarshaller = JAXBContext.newInstance(classOf[Appointments]).createUnmarshaller()
      xmlInputFactory = XMLInputFactory.newFactory()
    }

    @ProcessElement
    def processElement(c: ProcessContext): Unit = {
      val xmls = c.element().getValue.asScala
      xmls.foreach { message =>
        val attributes = message.getAttributeMap.asScala.toMap
        Try {
          Some(message.getPayload)
            .map(i => new ByteArrayInputStream(i))
            .map(i => xmlInputFactory.createXMLStreamReader(i))
            .map(i => new StreamReaderDelegate(i) {
              override def getAttributeNamespace(input: Int) = ""

              override def getNamespaceURI() = ""
            })
            .map(i => unmarshaller.unmarshal(i).asInstanceOf[Appointments])
            .orNull

        } match {
          case Success(value) => {
            c.output(apptKVSuccessTag, KV.of(value, message.getAttributeMap))
          }
          case Failure(exception) => {
            LOG.error(exception.getMessage)
            c.output(stringFailedTag, s"${message.getPayload.map(_.toChar).mkString},${metadata(attributes)}")
          }
        }
      }
    }
  }

  val assignRandomKeys = (shardsNumbers:Int) => new DoFn[PubsubMessage,KV[java.lang.Integer,PubsubMessage]]() {
    val random = new Random()

    @ProcessElement
    def processElement(c: ProcessContext): Unit = {
      c.output(KV.of( Integer.valueOf(random.nextInt(shardsNumbers)), c.element()))
    }
  }

  def build(subscription: ValueProvider[String])(implicit p: Pipeline, config: ValueProvider[String]): Unit = {

    transforms(20,1)(p.apply("pub/sub", PubsubIO.readMessagesWithAttributes().fromSubscription(subscription)))
      .apply(Sink.bigquerySinkDynamicStream("BigQuerySinkDynamicStream"))
  }

  def transforms(shardsnum: Int, batchSize: Int)(messages: PCollection[PubsubMessage])(implicit config: ValueProvider[String]): PCollection[TableRow] = {
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

    val appt =  messages.apply("adding-keys",ParDo.of(assignRandomKeys(shardsnum)))
      .apply("batch", GroupIntoBatches.ofSize(batchSize))
      .apply("mapping-to-appointment", ParDo.of(toApptFn())
        .withOutputTags(apptKVSuccessTag, TupleTagList.of(stringFailedTag)))


    appt.get(stringFailedTag).apply(Window.into(FixedWindows.of(Duration.standardMinutes(1))))
      .apply("failed records", writeToFile(errorPath.toValueProvider))

    appt.get(apptKVSuccessTag)
      .apply("appointment-data-to-json", MapElements.into(TypeDescriptors.kvs(TypeDescriptor.of(classOf[String]), TypeDescriptors.maps(TypeDescriptors.strings(), TypeDescriptors.strings())))
        .via((kv : KV[Appointments,java.util.Map[String,String]]) => {
          val json = mapper.writeValueAsString(kv.getKey)
          KV.of(json,kv.getValue)
        }))

      .apply("to tablerow", MapElements.into(TypeDescriptor.of(classOf[TableRow]))
        .via((kv: KV[String,java.util.Map[String,String]]) => {
          val tr = kv.getKey.toTableRow
          val attributes = kv.getValue.asScala.toMap

          tr.set("TopicName", attributes("TopicName"))
          tr.set("MessageId", attributes("MessageId"))
          tr.set("LogicalDate", attributes("LogicalDate"))
        }))


  }


}
