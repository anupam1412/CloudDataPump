package com.accenture.datastudio.transformers

import java.{lang, util}

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.api.services.bigquery.model.TableRow
import com.accenture.datastudio.core.templates._
import com.accenture.datastudio.transformers.helpers.Content
import com.accenture.datastudio.transformers.helpers.FieldOpInstance.FieldOpService._
import com.typesafe.config.Config
import org.apache.beam.sdk.options._
import org.apache.beam.sdk.transforms.DoFn.ProcessElement
import org.apache.beam.sdk.transforms.{PTransform, ParDo, _}
import org.apache.beam.sdk.values._
import org.slf4j.{Logger, LoggerFactory}

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}


trait Transformer[A,B]{
  def transform(input: A):B
}

case class InputPartitionDateFromField(newfieldName: ValueProvider[String], targetField:ValueProvider[String])

object TransformerInstance {

  val LOG: Logger = LoggerFactory.getLogger(TransformerInstance.getClass)

  implicit val jsonStrategy = new Transformer[Option[ValueProvider[java.util.List[Strategy]]], PTransform[PCollection[JsonNode], PCollectionTuple]] {

    override def transform(input: Option[ValueProvider[util.List[Strategy]]]): PTransform[PCollection[JsonNode], PCollectionTuple] = new PTransform[PCollection[JsonNode], PCollectionTuple] {
      val jsonStrategyTransformfn = (strategies: Option[ValueProvider[java.util.List[Strategy]]]) => new DoFn[JsonNode, JsonNode]() {
        @ProcessElement
        def processElement(c: ProcessContext): Unit = {
          val input = c.element()
          strategies match {
            case Some(v) if v.get().asScala.toList.nonEmpty => Try {
              JsonStrategy.updateJson(input, v.get().asScala.toList)
            } match {
              case Success(value) => c.output(jsonNodeSuccessTag, value)
              case Failure(exception) => {
                LOG.error(exception.getMessage)
                c.output(jsonNodeFailedTag, input)
              }
            }
            case Some(_) => c.output(jsonNodeSuccessTag, input)
            case None => c.output(jsonNodeSuccessTag, input)
          }
        }
      }

      override def expand(begin: PCollection[JsonNode]): PCollectionTuple = {
        begin.apply("jsonStrategy", ParDo.of(jsonStrategyTransformfn(input))
          .withOutputTags(jsonNodeSuccessTag, TupleTagList.of(jsonNodeFailedTag)))
      }
    }
  }

  implicit val csvToTableRow = new Transformer[ValueProvider[java.util.List[String]], PTransform[PCollection[java.lang.Iterable[KV[String,String]]], PCollectionTuple]] {
    override def transform(input: ValueProvider[util.List[String]]): PTransform[PCollection[lang.Iterable[KV[String, String]]], PCollectionTuple] = new PTransform[PCollection[java.lang.Iterable[KV[String,String]]], PCollectionTuple] {
      val csvToTableRowfn = () => new DoFn[java.lang.Iterable[KV[String,String]], TableRow](){
        @ProcessElement
        def processElement(context: ProcessContext ):Unit = {
          val lst = context.element().asScala.toList
          val element = lst.map(i => i.getValue)
          Try(CsvToTableRow.toTableRow(element,input.get().asScala.toList, new TableRow())) match {
            case Success(value) => {
              if(value.isDefined) {
                val tr = value.get
                  .set("bq_row_id" , java.util.UUID.randomUUID.toString)
                  .set("bq_row_src" , lst.head.getKey)

                context.output(tableRowSuccessTag, tr)
              }
            }
            case Failure(exception) =>
              LOG.error(s"Failed Transform. ${exception.getMessage}")
              context.output(stringFailedTag, element.mkString(","))
          }
        }

      }

      override def expand(input: PCollection[lang.Iterable[KV[String, String]]]): PCollectionTuple = {
        input.apply("CsvToTableRowFn", ParDo.of(csvToTableRowfn())
          .withOutputTags(tableRowSuccessTag, TupleTagList.of(stringFailedTag)))
      }
    }
  }

  implicit val partitionDateFromField = new Transformer[InputPartitionDateFromField,PTransform[PCollection[TableRow],PCollection[TableRow]]] {
   import com.accenture.datastudio.transformers.helpers.FieldOpFileTypeInstance.csvFile._

    override def transform(input: InputPartitionDateFromField): PTransform[PCollection[TableRow], PCollection[TableRow]] = new PTransform[PCollection[TableRow],PCollection[TableRow]] {

      override def expand(begin: PCollection[TableRow]): PCollection[TableRow] = {
        begin.apply(
          "CloneFieldTransform",
          MapElements.into(TypeDescriptor.of(classOf[TableRow])).via[TableRow]((tr: TableRow) => {

            val trName = Some(Content(input.targetField.get(), tr.get(input.targetField.get()).asInstanceOf[String]))
              .map(i => retrieveField(i))
              .orNull
              .name

            val res = Some(Content(input.targetField.get(), tr.get(trName).asInstanceOf[String]))
              .map(i => retrieveField(i))
              .map(i => retrieveBetweenParenthesis(i))
              .map(i => bqTransformTargetBy(i))

            tr.clone().set(input.newfieldName.get(), res.orNull.value)
          }))
      }


    }
  }

}

object JsonStrategy {
  import com.accenture.datastudio.transformers.helpers.FieldOpFileTypeInstance.jsonFile._

  val LOG: Logger = LoggerFactory.getLogger(JsonStrategy.getClass)

  def updateJson(json: JsonNode, strategies: List[Strategy]): JsonNode = {
    @tailrec def loop(strategies: List[Strategy], jsonUpdated: ObjectNode): JsonNode = {
      strategies match {
        case h :: t => {
          val jsonNodeTarget = jsonUpdated.at(h.location)
          if (!jsonNodeTarget.isMissingNode) {
            val jsonNodeTargetValue = jsonNodeTarget
            match {
              case node if node.isNull => None
              case node => Some(node.asText())

            }

            Try(executeFunction(jsonNodeTargetValue, h.operation)) match {
              case Success(value) => {
                val fieldname = h.location.split("/").last
                value match {
                  case r: String => jsonUpdated.put(fieldname, r)
                  case r: Long => jsonUpdated.put(fieldname, r)
                  case r: BigDecimal => {
                    jsonUpdated.put(fieldname, r.bigDecimal)
                  }
                  case _ => jsonUpdated.putNull(fieldname)
                }
              }
              case Failure(exception) => {
                LOG.error(exception.getMessage)
                throw exception
              }
            }
          }
          loop(t, jsonUpdated)
        }
        case Nil => jsonUpdated
      }
    }

    loop(strategies, json.deepCopy[ObjectNode]())
  }

  def executeFunction(value: Option[String], operation: String): Any = {
    value match {
      case Some(v) => {
        val res = Some(Content(operation, v))
          .map(i => retrieveField(i))
          .map(i => retrieveBetweenParenthesis(i))
          .map(i => bqTransformTargetBy(i))

        res.orNull.value

      }
      case None => null
    }
  }
}

object CsvToTableRow{

  import com.accenture.datastudio.transformers.helpers.FieldOpFileTypeInstance.csvFile._

  def toTableRow(line: List[String], headers: List[String], tableRow: TableRow): Option[TableRow] = {
    if(headers.size == line.size) {
      line.zipWithIndex.foreach {
        i => {
          val res = Some(Content(headers(i._2), i._1))
            .map(i => retrieveField(i))
            .map(i => retrieveBetweenParenthesis(i))
            .map(i => bqTransformTargetBy(i))

          tableRow.set(res.orNull.name, res.orNull.value)
        }
      }
    }else{
      throw new Exception("File schema doesn't match with number of columns within the line")
    }
    Some(tableRow)

  }
}

object Transformer {

  import com.accenture.datastudio.core.valueproviders.ValueProviderConfigExtProvider._
  import com.accenture.datastudio.core.valueproviders.ValueProviderConfigExtSyntax._

  def jsonStrategy(implicit transformer: Transformer[Option[ValueProvider[java.util.List[Strategy]]], PTransform[PCollection[JsonNode], PCollectionTuple]], config: ValueProvider[String]) = {
    implicit val root = Some("JsonStrategyTransform")
    val strategies = ((config: Config) => {
      val lst = config.getConfigList("jsonStrategy").asScala.toList
        .map(c => {
          val operation = c.getString("operation")
          Strategy(c.getString("location"), operation)
        })
      lst.asJava
    }).toValueProvider
    transformer.transform(Some(strategies))

  }

  def csvToTableRow(implicit transformer:Transformer[ValueProvider[java.util.List[String]], PTransform[PCollection[java.lang.Iterable[KV[String,String]]], PCollectionTuple]], config: ValueProvider[String]) = {
    implicit val root = Some("CsvToTableRowTransform")

    val headers = (config:Config) =>  Some(config.getString("headers"))
      .map(i => i.split(",").map(_.trim).toList).orNull.asJava

    transformer.transform(headers.toValueProvider)
  }

  def partitionDateFromField(implicit transformer:Transformer[InputPartitionDateFromField,PTransform[PCollection[TableRow],PCollection[TableRow]]], config: ValueProvider[String])  = {
    implicit val root =  Some("PartitionDateFromFieldTransform")

    val fieldName = "fieldName".toValueProvider
    val targetField = "targetField".toValueProvider
    transformer.transform(InputPartitionDateFromField(fieldName, targetField))
  }
}

case class Strategy(location:String, operation:String) extends Serializable
