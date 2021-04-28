package com.accenture.datastudio.template

import com.accenture.datastudio.core.io.{Sink, Source}
import com.accenture.datastudio.core.templates._
import com.accenture.datastudio.core.io.SinkInstance._
import com.accenture.datastudio.transformers.Transformer
import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.options.ValueProvider
import org.apache.beam.sdk.transforms.windowing.{FixedWindows, Window}
import org.apache.beam.sdk.values.{PCollection, PCollectionTuple}
import org.joda.time.Duration
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.Write.WriteDisposition
import com.accenture.datastudio.core.io.SourceInstance._
import  com.accenture.datastudio.transformers.TransformerInstance._

object CSVBatchPipeline {

  import com.accenture.datastudio.core.valueproviders.ValueProviderConfigExtProvider._
  import com.accenture.datastudio.core.valueproviders.ValueProviderConfigExtSyntax._

  def build(filePath: ValueProvider[String], writeDisposition: WriteDisposition)(implicit p: Pipeline, config: ValueProvider[String]): Unit = {

    implicit val root = None

    val csvtr = transforms(filePath)

    csvtr.get(stringFailedTag).apply(Window.into(FixedWindows.of(Duration.standardMinutes(1))))
      .apply("failed records", writeToFile(errorPath.toValueProvider))

    csvtr.get(tableRowSuccessTag).apply("BigQuery", Sink.biqquerySinkDynamic(writeDisposition))

  }

  def transforms(filePath: ValueProvider[String])(implicit p: Pipeline,config: ValueProvider[String]):PCollectionTuple = {


    p.apply("fileSource", Source.inputFileSource(filePath)).apply("csv2tablerow", Transformer.csvToTableRow)

  }
}
