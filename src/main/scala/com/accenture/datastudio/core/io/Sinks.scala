package com.accenture.datastudio.core.io

import com.google.api.services.bigquery.model.{TableRow, TimePartitioning}
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.Write.{CreateDisposition, Method, WriteDisposition}
import org.apache.beam.sdk.io.gcp.bigquery._
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO
import org.apache.beam.sdk.options.ValueProvider
import org.apache.beam.sdk.options.ValueProvider.NestedValueProvider
import org.apache.beam.sdk.transforms.{MapElements, PTransform, SerializableFunction}
import org.apache.beam.sdk.values.{PCollection, PDone, TypeDescriptor, ValueInSingleWindow}
import org.joda.time.{Duration, Instant}
import org.slf4j.{Logger, LoggerFactory}

trait Sink[A,B] {

  def done(input: A):B

}

case class BigqueryStaticConfig(writeDisposition: WriteDisposition)
case class InputsBigQuerySinkDynamic(table: ValueProvider[String], loadTimeField:ValueProvider[String], pField:ValueProvider[String])
case class BigqueryConfig(bigqueryStaticConfig: BigqueryStaticConfig, inputsBigQuerySinkDynamic: InputsBigQuerySinkDynamic)

case class InputsBigQuerySinkDynamicStream(inputsBigQuerySinkDynamic:InputsBigQuerySinkDynamic, customGcsTempLocation: ValueProvider[String])

object SinkInstance {

  implicit val biqquerySinkDynamic = new Sink[BigqueryConfig, PTransform[PCollection[TableRow], WriteResult]] {
    override def done(input: BigqueryConfig): PTransform[PCollection[TableRow], WriteResult] = new PTransform[PCollection[TableRow], WriteResult] {

      val LOG: Logger = LoggerFactory.getLogger(this.getClass.getName)

      override def expand(begin: PCollection[TableRow]): WriteResult = {
        val bqtr =   BigQueryIO.writeTableRows()
          .to(SerializableFunctionImpl.partitionfield(input.inputsBigQuerySinkDynamic))
          .withExtendedErrorInfo()
          .withCreateDisposition(CreateDisposition.CREATE_NEVER)
          .withWriteDisposition(input.bigqueryStaticConfig.writeDisposition)

        begin.apply(
          MapElements.into(TypeDescriptor.of(classOf[TableRow])).
            via[TableRow](SerializableFunctionImpl.loadTimeField(input.inputsBigQuerySinkDynamic))).apply("write-to-bigquery", bqtr)
      }
    }
  }

  implicit val bigquerySinkDinamicStream = new Sink[InputsBigQuerySinkDynamicStream, PTransform[PCollection[TableRow], WriteResult]] {

    override def done(input: InputsBigQuerySinkDynamicStream): PTransform[PCollection[TableRow], WriteResult] = new PTransform[PCollection[TableRow], WriteResult] {
      override def expand(begin: PCollection[TableRow]): WriteResult = {

        val bqWriteTableRows = BigQueryIO.writeTableRows()
          .to(SerializableFunctionImpl.partitionfield(input.inputsBigQuerySinkDynamic))
          .withTriggeringFrequency(Duration.standardMinutes(1))
          .withMethod(Method.FILE_LOADS)
          .withNumFileShards(200)
          .withCreateDisposition(CreateDisposition.CREATE_NEVER)
          .withWriteDisposition(WriteDisposition.WRITE_APPEND)

          .withCustomGcsTempLocation(NestedValueProvider.of[String, String](input.customGcsTempLocation, (str: String) =>
            if (str.nonEmpty) str
            else null
          ))

        begin.apply(
          MapElements.into(TypeDescriptor.of(classOf[TableRow])).
            via[TableRow](SerializableFunctionImpl.loadTimeField(input.inputsBigQuerySinkDynamic))).
          apply("Write to BigQuery Dynamic Stream", bqWriteTableRows)
      }
    }
  }

  implicit val pubsubSink = new Sink[ValueProvider[String], PTransform[PCollection[String], PDone]] {
    override def done(input: ValueProvider[String]): PTransform[PCollection[String], PDone] = new PTransform[PCollection[String], PDone] {
      override def expand(begin: PCollection[String]): PDone = {
        begin.apply("P/S sink", PubsubIO.writeStrings().to(input))
      }
    }
  }
}


object SerializableFunctionImpl{

  def partitionfield(inputsBigQuerySinkDynamic: InputsBigQuerySinkDynamic): SerializableFunction[ValueInSingleWindow[TableRow], TableDestination] = (input: ValueInSingleWindow[TableRow]) => {
    if (inputsBigQuerySinkDynamic.pField.get().nonEmpty) {
      val DAY_PARTITION = new TimePartitioning().setType("DAY").setField(inputsBigQuerySinkDynamic.pField.get())
      new TableDestination(inputsBigQuerySinkDynamic.table.get() + "$" + s"${input.getValue.get(inputsBigQuerySinkDynamic.pField.get()).toString.replaceAll("-", "")}", null, DAY_PARTITION)

    } else {
      new TableDestination(inputsBigQuerySinkDynamic.table.get(), null)
    }
  }

  def loadTimeField(inputsBigQuerySinkDynamic: InputsBigQuerySinkDynamic): SerializableFunction[TableRow, TableRow] = (input: TableRow) => {
    if (inputsBigQuerySinkDynamic.loadTimeField.get().nonEmpty) {
      val newTableRow = input
      newTableRow.set(inputsBigQuerySinkDynamic.loadTimeField.get(), Instant.now().toString)
    } else {
      input
    }
  }
}

object Sink {
  import com.accenture.datastudio.core.valueproviders.ValueProviderConfigExtProvider._
  import com.accenture.datastudio.core.valueproviders.ValueProviderConfigExtSyntax._

  def biqquerySinkDynamic(writeDisposition: WriteDisposition)(implicit sink: Sink[BigqueryConfig, PTransform[PCollection[TableRow], WriteResult]] , config: ValueProvider[String]): PTransform[PCollection[TableRow], WriteResult] = {
    implicit val root = Some("BigQuerySinkDynamic")

    val table = "table".toValueProvider
    val pField = "pField".toValueProvider
    val loadTimeField = "loadTimeField".toValueProvider
    sink.done(BigqueryConfig(BigqueryStaticConfig(writeDisposition),InputsBigQuerySinkDynamic(table, loadTimeField, pField)))
  }

  def bigquerySinkDynamicStream(rootConfig:String)(implicit sink: Sink[InputsBigQuerySinkDynamicStream, PTransform[PCollection[TableRow], WriteResult]] , config: ValueProvider[String]): PTransform[PCollection[TableRow], WriteResult] = {
    implicit val root = Some(rootConfig)
    val table = "table".toValueProvider
    val loadTimeField = "loadTimeField".toValueProvider
    val pField = "pField".toValueProvider
    val customGcsTempLocation = "customGcsTempLocation".toValueProvider
    sink.done(InputsBigQuerySinkDynamicStream(InputsBigQuerySinkDynamic(table, loadTimeField,pField),customGcsTempLocation))
  }

  def pubsubSink(implicit sink: Sink[ValueProvider[String],PTransform[PCollection[String], PDone]] , config: ValueProvider[String]): PTransform[PCollection[String], PDone] = {
    implicit val root = Some("PubsubSink")
    sink.done("topic".toValueProvider)
  }
}
