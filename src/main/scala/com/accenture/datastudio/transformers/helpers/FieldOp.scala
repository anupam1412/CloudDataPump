package com.accenture.datastudio.transformers.helpers

import com.accenture.datastudio.transformers.helpers.BigqueryTransformsInstance.bqTransformService._

import scala.util.{Failure, Success, Try}

case class Content(metadata:String, content:String)
case class FieldTypeContent(name:String,fieldType:String, content:Any)
case class FieldInputOp(fieldTypeContent:FieldTypeContent, inputOp:String)
case class FieldValuePair(name:String, value:Any)

trait FieldOpFileType{
  def retrieveField(input:Content): FieldTypeContent
}

object FieldOpFileTypeInstance{
  val csvFile = new FieldOpFileType {
    override def retrieveField(input: Content): FieldTypeContent = {
      Try {
        val Array(fieldName, datatype) = input.metadata.split(":\\s*(?![^()]*\\))")
        (fieldName, datatype)
      } match {
        case Success(v) => FieldTypeContent(v._1, v._2, input.content)
        case Failure(e) => {
          throw new Exception(e.getMessage)
        }
      }
    }
  }
  val jsonFile = new FieldOpFileType {
    override def retrieveField(input: Content): FieldTypeContent = {
      FieldTypeContent("", input.metadata, input.content)
    }
  }
}



trait FieldOp{
  def retrieveBetweenParenthesis(field:FieldTypeContent): FieldInputOp
  def bqTransformTargetBy(field:FieldInputOp): FieldValuePair


}

object FieldOpInstance {
  val FieldOpService = new FieldOp {

    override def retrieveBetweenParenthesis(input:FieldTypeContent): FieldInputOp = {
      val text = input.fieldType
      if(text.contains("(")) {
        val ext = text.substring(text.indexOf("(")+1,text.indexOf(")"))
        FieldInputOp(input, ext)
      }
      else FieldInputOp(input, "")
    }

    override def bqTransformTargetBy(input: FieldInputOp): FieldValuePair = {
      val content = input.fieldTypeContent.content.toString
      val fieldType = input.fieldTypeContent.fieldType
      val fieldName = input.fieldTypeContent.name
      val contentExtracted = input.inputOp

      val res =  Option(content) match {
        case Some(value) if value.isEmpty => FieldValuePair(fieldName, null)
        case None => FieldValuePair(fieldName, null)
        case Some(value) => {

          val map = Try(fieldType match {
            case "STRING" => value
            case "INTEGER" => value.toInt
            case "DOUBLE" => value.toDouble
            case "int2bigDecimal" => int2bigDecimal(value)

            case "FLOAT" => value.toFloat
            case "LONG" => value.toLong
            case "BIGINT" => BigInt(value).bigInteger
            case "BIGDECIMAL" => BigDecimal(value).bigDecimal
            case t: String if t.matches("^TIMESTAMP.*") => {
              toZoneBqTimestampString(value, contentExtracted)
            }
            case t: String if t.matches("^DATE.*") => {

              toBqDateString(value, contentExtracted)
            }
            case t: String if t.matches("^PRECISION.*") => {
              decimalRoundUp(BigDecimal(value), contentExtracted.toInt)

            }
            case t: String if t.matches("^UTC_TIMESTAMP.*") => {
              toLocalDateTime(value, contentExtracted).toString

            }
            case t: String if t.matches("^TIME.*") => {
              toBqTimeString(value, contentExtracted)

            }
            case t if t.matches("^TRANSFORM_TIMESTAMP_TO_DATE.*") => {
              toZoneBqLocalDateString(value, contentExtracted)
            }
            case t if t.matches("^TRANSFORM_UTC_TIMESTAMP_TO_DATE.*") => {
              toLocalDateTime(value, contentExtracted).toLocalDate.toString
            }

            case _ => throw new Exception("Data type not supported")
          })
          map match {
            case Success(v) => FieldValuePair(fieldName, v)
            case Failure(exception) => throw new Exception(s"failed at field: ${fieldName}, trace:${exception.getMessage}")
          }
        }
      }
      res
  }

}
}