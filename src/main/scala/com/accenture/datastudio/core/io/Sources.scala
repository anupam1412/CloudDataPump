package com.accenture.datastudio.core.io

import java.io.{BufferedReader, InputStreamReader, StringReader}
import java.lang
import java.nio.channels.Channels
import java.nio.file.Files

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.{JsonNodeFactory, ObjectNode}
import com.google.api.services.bigquery.model.TableRow
import com.accenture.datastudio.core.JsonNodeCoder
import com.accenture.datastudio.core.valueproviders.{DualInputNestedValueProvider, TranslatorInput}
import com.typesafe.config.Config
import com.univocity.parsers.csv.{CsvParser, CsvParserSettings}
import org.apache.avro.file.DataFileStream
import org.apache.avro.generic.{GenericDatumReader, GenericRecord}
import org.apache.beam.sdk.coders.{NullableCoder, StringUtf8Coder}
import org.apache.beam.sdk.io.FileIO.ReadableFile
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO
import org.apache.beam.sdk.io.{Compression, FileIO, FileSystems, ReadableFileCoder}
import org.apache.beam.sdk.options.ValueProvider
import org.apache.beam.sdk.options.ValueProvider.NestedValueProvider
import org.apache.beam.sdk.transforms.DoFn.{ProcessElement, Setup}
import org.apache.beam.sdk.transforms._
import org.apache.beam.sdk.values._
import org.codehaus.jackson.map.MappingIterator

import scala.collection.JavaConverters._


trait Source[A,B] {

  def begin(input: A):B

}

case class AvroSourceInputs(filePath:ValueProvider[String], path: ValueProvider[String], partition:ValueProvider[Partition])
case class FileSourceInputs(filePath:ValueProvider[String], location:ValueProvider[Location], splitter:ValueProvider[String],
                            headerRegex:ValueProvider[String],trailerRegex:ValueProvider[String])

object SourceInstance  {
  implicit val avroSource = new Source[AvroSourceInputs, PTransform[PBegin, PCollection[JsonNode]]]  {
    override def begin(input: AvroSourceInputs): PTransform[PBegin, PCollection[JsonNode]] = new PTransform[PBegin, PCollection[JsonNode]](){
      implicit lazy val json = JsonNodeFactory.instance

      val pathToUse = DualInputNestedValueProvider.of[String, String, String](input.path, input.filePath,
        (ti: TranslatorInput[String, String]) => {
          Option(ti.y) match{
            case Some(v) if v.nonEmpty => v
            case Some(v) if v.isEmpty => ti.x
            case None => ti.x
          }
        })

      val avroToJsonNodeFn = () => new DoFn[ReadableFile, JsonNode](){

        import com.accenture.datastudio.transformers.helpers.JsonNodeSyntax._
        import com.accenture.datastudio.transformers.helpers.JsonNodeWriterInstance._

        @ProcessElement
        def processElement(context: ProcessContext ):Unit = {

          Some(context.element())
            .map(i => FileSystems.open(FileSystems.matchNewResource(i.getMetadata.resourceId().toString, false)))
            .map(i => new DataFileStream[GenericRecord](Channels.newInputStream(i), new GenericDatumReader[GenericRecord]()))
            .foreach(i =>
              i.forEach {
                gr =>
                  val date = extractFromRegex(Some(input.partition.get().extractDate), context.element().getMetadata.resourceId().toString)
                  val json = gr.toJson.asInstanceOf[ObjectNode]
                  if (date.nonEmpty) {
                    json.put(input.partition.get().pFieldName, date)
                  }
                  json.put("bq_row_id", java.util.UUID.randomUUID.toString)
                  json.put("bq_row_src", context.element().getMetadata.resourceId().toString)
                  context.output(json)
              })
        }
      }

      override def expand(begin: PBegin): PCollection[JsonNode] = {
        begin
          .apply("FileSource - Read", FileIO.`match`().filepattern(pathToUse))
          .apply(FileIO.readMatches().withCompression(Compression.AUTO)).setCoder(ReadableFileCoder.of())
          .apply(ParDo.of(avroToJsonNodeFn())).setCoder(JsonNodeCoder.of)
      }
    }

  }

  implicit val bigquerySource = new Source[ValueProvider[String], PTransform[PBegin, PCollection[TableRow]]]  {
    override def begin(input: ValueProvider[String]): PTransform[PBegin, PCollection[TableRow]] = new PTransform[PBegin, PCollection[TableRow]](){
      override def expand(begin: PBegin): PCollection[TableRow] = {
        begin.apply("Read from query", BigQueryIO.readTableRows().withoutValidation().fromQuery(input).usingStandardSql().withTemplateCompatibility())
      }
    }
  }

  implicit val fileSource = new Source[FileSourceInputs, PTransform[PBegin, PCollection[java.lang.Iterable[KV[String,String]]]]]  {
    override def begin(input: FileSourceInputs): PTransform[PBegin, PCollection[lang.Iterable[KV[String, String]]]] = new PTransform[PBegin, PCollection[java.lang.Iterable[KV[String,String]]]](){
      val path =  DualInputNestedValueProvider.of[String, Location, String](input.location, input.filePath,
        (ti: TranslatorInput[Location, String]) => {
          Option(ti.y) match{
            case Some(v) if v.nonEmpty => v
            case Some(v) if v.isEmpty => ti.x.path
            case None => ti.x.path
          }
        })


      val regex = NestedValueProvider.of[String,Location](input.location, (l: Location) => l.regex)

      val extractDateFn = () => new DoFn[ReadableFile, KV[String,KV[String,String]]](){
        @ProcessElement
        def processElement(context: ProcessContext ):Unit = {
          val rf =  context.element()
          val fs = FileSystems.open(FileSystems.matchNewResource(rf.getMetadata.resourceId().toString, false))
          val stream = Channels.newInputStream(fs)
          try {
            val streamReader = new BufferedReader(new InputStreamReader(stream, "UTF-8"))
            streamReader.lines().forEach(l => {
              val date = extractFromRegex(Some(regex.get()), rf.getMetadata.resourceId().toString)
              val resline = if (date.nonEmpty) KV.of(l, date) else KV.of(l, "")
              context.output(KV.of(rf.getMetadata.resourceId().toString, resline))
            })
          }finally {
            stream.close()
          }
        }
      }

      val splitterFn = () => new DoFn[KV[String,KV[String,String]], java.lang.Iterable[KV[String,String]]]() {

        var settings:CsvParserSettings = _

        @Setup
        def init: Unit =  {
          settings = new CsvParserSettings()
        }

        @ProcessElement
        def processElement(context: ProcessContext ):Unit = {
          settings.setNullValue("")
          settings.setDelimiterDetectionEnabled(true, input.splitter.get().head)
          val mapper = new CsvParser(settings)
          val c = context.element()
          val partitionDate = c.getValue.getValue
          val filename = c.getKey
          val d = mapper.parseAll(new StringReader(context.element().getValue.getKey)).asScala.toList
          val columns =  d.flatten.map{i => KV.of(filename, i)}
          val res: Seq[KV[String, String]] = if(partitionDate.isEmpty){
            columns
          }else{
            columns:+KV.of(filename,partitionDate)
          }
          context.output(res.asJava)
        }
      }

      val removeHeaderOrTrailerfn = () => new DoFn[KV[String,KV[String,String]], KV[String,KV[String,String]]](){
        @ProcessElement
        def processElement(context: ProcessContext ):Unit = {
          val element =  context.element()
          if(!hasHeaderOrTrailer(element.getValue.getKey, Option(input.headerRegex.get()),Option(input.trailerRegex.get()))){
            context.output(element)
          }
        }
      }



      override def expand(begin: PBegin): PCollection[lang.Iterable[KV[String, String]]] = {
        begin
          .apply("FileSource - Read", FileIO.`match`().filepattern(path))
          .apply(FileIO.readMatches().withCompression(Compression.AUTO)).setCoder(ReadableFileCoder.of())
          .apply("extract-date-if-exist", ParDo.of(extractDateFn()))
          .apply("RemoveHeaderOrTrailer", ParDo.of(removeHeaderOrTrailerfn()))
          .apply("splitter", ParDo.of(splitterFn()))
      }
    }
  }



  def extractFromRegex(regex: Option[String], path: String): String = {
    regex match {
      case Some(v) if v.isEmpty => ""
      case Some(v) if v.nonEmpty => val rx = v.r.unanchored
        path match {
          case rx(date) => date
        }

      case None => ""
    }
  }

  def hasHeaderOrTrailer(line : String, headerRegex: Option[String], trailerRegex:Option[String]): Boolean ={

    val h = headerRegex.fold(false) (regex => line.matches(regex))
    val t = trailerRegex.fold(false)(regex => line.matches(regex))
    h || t
  }

}

object Source {

  import com.accenture.datastudio.core.valueproviders.ValueProviderConfigExtProvider._
  import com.accenture.datastudio.core.valueproviders.ValueProviderConfigExtSyntax._

  def inputAvro(filePath: ValueProvider[String])(implicit source: Source[AvroSourceInputs, PTransform[PBegin, PCollection[JsonNode]]], config: ValueProvider[String]): PTransform[PBegin, PCollection[JsonNode]] = {
    implicit val root = Some("AvroSource")
    val partitionProvider = ((config: Config) => {
      val locationConfig = config.getObject("partition").toConfig
      Partition(locationConfig.getString("extractDateRegex"), locationConfig.getString("pFieldName"))
    }).toValueProvider

    val input = AvroSourceInputs(filePath, "path".toValueProvider, partitionProvider)
    source.begin(input)
  }

  def inputBigquery(implicit source: Source[ValueProvider[String], PTransform[PBegin, PCollection[TableRow]]], config: ValueProvider[String]): PTransform[PBegin, PCollection[TableRow]] = {
    implicit val root = Some("BigquerySource")
    source.begin("query".toValueProvider)

  }

  def inputFileSource(filePath:ValueProvider[String])(implicit source: Source[FileSourceInputs, PTransform[PBegin, PCollection[java.lang.Iterable[KV[String,String]]]]], config: ValueProvider[String]) = {
    implicit val root = Some("FileSource")

    val locationProvider = ((config: Config) => {
      val locationConfig = config.getObject("location").toConfig
      Location(locationConfig.getString("path"), locationConfig.getString("extractDateRegex"))
    }).toValueProvider
    source.begin(FileSourceInputs(filePath,locationProvider,"splitter".toValueProvider , "headerRegex".toValueProvider, "trailerRegex".toValueProvider))
  }
}

case class Partition(extractDate: String, pFieldName: String)
case class Location(path: String, regex:String)
