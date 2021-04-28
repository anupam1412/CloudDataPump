package com.accenture.datastudio.core

import com.fasterxml.jackson.databind.JsonNode
import com.google.api.services.bigquery.model.TableRow
import org.apache.beam.sdk.io.TextIO
import org.apache.beam.sdk.options.ValueProvider
import org.apache.beam.sdk.values.{KV, TupleTag}

package object templates {

  val jsonNodeSuccessTag: TupleTag[JsonNode] = new TupleTag[JsonNode]() {}
  val jsonNodeFailedTag: TupleTag[JsonNode] = new TupleTag[JsonNode]() {}

  val tableRowSuccessTag: TupleTag[TableRow] = new TupleTag[TableRow]() {}
  val jsonStringKVSuccessTag: TupleTag[KV[String, java.util.Map[String, String]]] = new TupleTag[KV[String, java.util.Map[String, String]]]() {}
  val stringFailedTag: TupleTag[String] = new TupleTag[String]() {}


  val writeToFile = (bucket: ValueProvider[String]) => {
    TextIO.write()
      .withNumShards(10)
      .withWindowedWrites()
      .to(bucket).withSuffix(".txt")
  }


  val metadata = (attributes: Map[String, String]) =>
    s"${attributes("TopicName")},${attributes("MessageId")},${attributes("LogicalDate")}"
}