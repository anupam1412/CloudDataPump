package com.accenture.datastudio.core.templates

import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions
import org.apache.beam.sdk.options.Validation.Required
import org.apache.beam.sdk.options.{Description, ValueProvider}

trait PipelineBuilderOptions extends DataflowPipelineOptions{

  @Description("Config")
  @Required
  def getConfig:ValueProvider[String]
  def setConfig(value:ValueProvider[String])

  @Description("Subscription name")
  def getSubscription:ValueProvider[String]
  def setSubscription(value:ValueProvider[String])

  @Description("File path")
  def getFilePath:ValueProvider[String]
  def setFilePath(value:ValueProvider[String])

  @Description("BQ write Disposition")
  def getWriteDisposition:String
  def setWriteDisposition(value:String)

  @Description("Ingestion mode")
  def getIngestionMode:String
  def setIngestionMode(value:String)


}

