package com.accenture.datastudio.transformers.helpers

import java.nio.ByteBuffer

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import org.apache.avro.data.TimeConversions
import org.apache.avro.data.TimeConversions.TimeConversion
import org.apache.avro.generic.GenericRecord
import org.apache.avro.{Conversions, LogicalTypes, Schema}

import scala.annotation.tailrec
import scala.collection.JavaConverters._



trait JsonWriter[A,B] {
  def write(input: A):B
}

object JsonNodeWriterInstance{
  implicit val genericRecordWriter = new JsonWriter[GenericRecord,JsonNode] {
    override def write(input: GenericRecord): JsonNode = {
      val json = JsonNodeFactory.instance.objectNode()
      @tailrec def loop(schema: Schema, field: Schema.Field,  record: GenericRecord):  JsonNode =  {
        Option(record.get(field.name())) match {
          case Some(value) => {
            val fieldName = field.name().toLowerCase
            schema.getType match {
              case Schema.Type.ENUM => json.put(fieldName, value.toString)
              case Schema.Type.INT  => {
                if(schema.getLogicalType!= null && schema.getLogicalType.getName == "date" ) {
                  val dateConversion = new TimeConversions.DateConversion()
                  val date = dateConversion.fromInt(value.asInstanceOf[Int], schema, schema.getLogicalType)
                  json.put(fieldName, date.toString)
                }
                else if (schema.getLogicalType!= null && schema.getLogicalType.getName == "time-millis" ) {
                  val dateConversion =  new TimeConversion()
                  val date = dateConversion.fromInt(value.asInstanceOf[Int], schema, schema.getLogicalType)
                  json.put(fieldName, date.toString)
                }
                else{
                  json.put(fieldName, value.asInstanceOf[Int])
                }
              }
              case Schema.Type.STRING  => json.put(fieldName,value.toString)
              case Schema.Type.LONG  =>
                if(schema.getLogicalType!= null && schema.getLogicalType.getName == "timestamp-millis" ) {
                  val dateConversion = new TimeConversions.TimestampConversion()
                  val date = dateConversion.fromLong(value.asInstanceOf[Long], schema, schema.getLogicalType)
                  json.put(fieldName, date.toString)

                }else{
                  json.put(fieldName, value.asInstanceOf[Long])

                }
              case Schema.Type.BYTES  =>  {
                if(schema.getLogicalType!= null && schema.getLogicalType.getName == "decimal") {
                  val decimalConversion = new Conversions.DecimalConversion
                  val props = schema.getObjectProps.asScala.toMap
                  val decimalType = LogicalTypes.decimal(props("precision").asInstanceOf[Int], props("scale").asInstanceOf[Int])
                  val bd = decimalConversion.fromBytes(value.asInstanceOf[ByteBuffer], schema, decimalType)
                  json.put(fieldName, bd)
                }else{
                  json.put(fieldName, value.asInstanceOf[Byte])
                }
              }
              case Schema.Type.FLOAT => json.put(fieldName, value.asInstanceOf[Float])
              case Schema.Type.DOUBLE => json.put(fieldName, value.asInstanceOf[Double])
              case Schema.Type.UNION => loop(field.schema().getTypes.get(1), field, record)
              case _ => throw new Exception("Field type not supported")
            }
          }
          case None => json.putNull(field.name().toLowerCase)
        }

      }
      val fields = input.getSchema.getFields.asScala
      fields.map{
        i => loop(i.schema(), i, input)
      }
      json
    }
  }
}

object JsonNodeSyntax {
  implicit class JsonWriterOps[A,B](value: A) {
    def toJson(implicit w:JsonWriter[A,B]):B = {
      w.write(value)
    }
  }

}

