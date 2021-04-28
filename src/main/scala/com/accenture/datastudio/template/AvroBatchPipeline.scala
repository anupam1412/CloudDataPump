package com.accenture.datastudio.template

import com.accenture.datastudio.core.JsonNodeCoder
import com.accenture.datastudio.core.io.{Sink, Source}
import com.accenture.datastudio.core.templates._
import com.accenture.datastudio.core.io.SinkInstance._
import com.accenture.datastudio.transformers.Transformer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.google.api.services.bigquery.model.TableRow
import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.options.ValueProvider
import org.apache.beam.sdk.transforms.MapElements
import org.apache.beam.sdk.transforms.windowing.{FixedWindows, Window}
import org.apache.beam.sdk.values.{PCollection, TypeDescriptor, TypeDescriptors}
import org.joda.time.Duration
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.Write.WriteDisposition
import com.accenture.datastudio.core.io.SourceInstance._
import  com.accenture.datastudio.transformers.TransformerInstance._
import com.accenture.datastudio.transformers.helpers.TableRowSyntax._
import com.accenture.datastudio.transformers.helpers.TableRowWriterInstance._

object AvroBatchPipeline {

  import com.accenture.datastudio.core.valueproviders.ValueProviderConfigExtProvider._
  import com.accenture.datastudio.core.valueproviders.ValueProviderConfigExtSyntax._

  implicit lazy val json: JsonNodeFactory = JsonNodeFactory.instance

  def build(filePath: ValueProvider[String], writeDisposition: WriteDisposition)(implicit p: Pipeline, config: ValueProvider[String]): Unit = {

    p.getCoderRegistry.registerCoderForClass(classOf[JsonNode], JsonNodeCoder.of)

    transforms(filePath).apply("BigQuerySinkDynamic", Sink.biqquerySinkDynamic(writeDisposition))
  }

  def transforms(filePath: ValueProvider[String])(implicit  p: Pipeline, config: ValueProvider[String]): PCollection[TableRow] ={

    implicit val root = None

    val js = p.apply("AvroSource", Source.inputAvro(filePath)).apply(Transformer.jsonStrategy)

    js.get(jsonNodeFailedTag)
      .apply(MapElements.into(TypeDescriptors.strings())
        .via((json:JsonNode) => json.toString))
      .apply(Window.into(FixedWindows.of(Duration.standardMinutes(1))))
      .apply("failed records", writeToFile(errorPath.toValueProvider))


    js.get(jsonNodeSuccessTag)
      .apply("Json->TableRow", MapElements.into(TypeDescriptor.of(classOf[TableRow]))
        .via((input: JsonNode) => input.toTableRow))


  }
}
