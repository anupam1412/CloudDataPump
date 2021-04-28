package com.accenture.datastudio

import com.accenture.datastudio.constants.Ingest
import com.accenture.datastudio.core.templates.PipelineBuilderOptions
import com.accenture.datastudio.template.{AvroBatchPipeline, CSVBatchPipeline, JsonStreamingPipeline, XmlStreamingPipeline}
import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.Write.WriteDisposition
import org.apache.beam.sdk.options.{PipelineOptionsFactory, ValueProvider}
import org.slf4j.{Logger, LoggerFactory}

/**
 * Starting point for every template/pipeline
 *
 */
object Application {
  def main(args: Array[String]): Unit = {

    val options = PipelineOptionsFactory.fromArgs(args: _*).withValidation().as(classOf[PipelineBuilderOptions])
    implicit val pipeline: Pipeline = Pipeline.create(options)
    implicit val config: ValueProvider[String] = options.getConfig
    val LOG: Logger = LoggerFactory.getLogger(this.getClass)


    options.getIngestionMode match {

      case Ingest.CSV_BATCH => CSVBatchPipeline.build(options.getFilePath, WriteDisposition.WRITE_APPEND)
      case Ingest.AVRO_BATCH => AvroBatchPipeline.build(options.getFilePath, WriteDisposition.WRITE_APPEND)
      case Ingest.JSON_STREAM => JsonStreamingPipeline.build(options.getSubscription)
      case Ingest.XML_STREAM => XmlStreamingPipeline.build(options.getSubscription)
      case _ => LOG.error("please specify an ingestion mode or specified ingestion mode is invalid.")

    }

    pipeline.run()
  }

}
