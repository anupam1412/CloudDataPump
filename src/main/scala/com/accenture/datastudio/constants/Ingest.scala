package com.accenture.datastudio.constants

object Ingest extends Enumeration {

  val CSV_BATCH = "csv_batch"
  val AVRO_BATCH= "avro_batch"
  val JSON_STREAM = "json_stream"
  val XML_STREAM = "xml_stream"
  val TEMPLATE_BUILD="build"
}
