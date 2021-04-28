package com.accenture.datastudio.transformers.helpers

import java.time._
import java.time.format.DateTimeFormatter

import org.joda.time.DateTime

import scala.math.BigDecimal.RoundingMode
import scala.util.{Failure, Success, Try}

trait BigqueryTransforms {
  def fromStringToJodaLocalDate(input:String, pattern:String): org.joda.time.LocalDate
  def fromStringToJodaInstant(input:String): org.joda.time.Instant
  def toZoneBqTimestampString(input: String, pattern:String): String
  def toZoneBqLocalDateString(input: String, pattern:String): String
  def toLocalDateTime(input: String, pattern:String) : LocalDateTime
  def toUTCZoneDateTime(input: ZonedDateTime): ZonedDateTime
  def decimalRoundUp(input: Any, scale:Int): String
  def int2bigDecimal(input: String): String
  def toBqDateString(input: String, pattern:String): String
  def toBqTimeString(input:String, pattern:String): String
  def fromTimestampToBqDateString(input: String, pattern:String): String


}

object BigqueryTransformsInstance{

  val bqTransformService = new BigqueryTransforms {

    def fromStringToJodaLocalDate(input:String, pattern:String): org.joda.time.LocalDate = {
      Option(input) match {
        case Some(v) =>
          val formatter = org.joda.time.format.DateTimeFormat.forPattern(pattern)
          org.joda.time.LocalDate.parse(v, formatter)
        case None => null
      }
    }

    def fromStringToJodaInstant(input:String): org.joda.time.Instant = {
      new DateTime(input).toInstant
    }


    def toZoneBqTimestampString(input: String, pattern:String): String = {
      toUTCZoneDateTime(toZoneDateTime(input, pattern)).toLocalDateTime.toString
    }

    def toZoneBqLocalDateString(input: String, pattern:String): String = {
      toUTCZoneDateTime(toZoneDateTime(input, pattern)).toLocalDate.toString
    }

    def toLocalDateTime(input: String, pattern:String) : LocalDateTime = {
      LocalDateTime.parse(input, DateTimeFormatter.ofPattern(pattern))
    }


    def toZoneDateTime(input: String, patter:String): ZonedDateTime = {
      ZonedDateTime.parse(input, DateTimeFormatter.ofPattern(patter))
    }

    def toUTCZoneDateTime(input: ZonedDateTime): ZonedDateTime = {
      ZonedDateTime.ofInstant(input.toInstant, ZoneId.of("UTC"))
    }

    def decimalRoundUp(input: Any, scale:Int): String = {
      val bd =  input match {
        case d:Double => BigDecimal.decimal(d)
        case d:Float => BigDecimal.decimal(d)
        case bd: BigDecimal => bd
      }
      bd.setScale(scale,RoundingMode.HALF_UP).bigDecimal.toPlainString

    }

    def int2bigDecimal(input: String): String = {
      val result = Try(input.toInt) match{
        case Success(value) => BigDecimal.int2bigDecimal(value).bigDecimal.toPlainString
        case Failure(exception) => {
          BigDecimal(input).bigDecimal.toPlainString
        }
      }
      result
    }

    def toBqDateString(input: String, pattern:String): String = {
      val localDate = LocalDate.parse(input, DateTimeFormatter.ofPattern(pattern))
      DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("UTC")).format(localDate)
    }

    def toBqTimeString(input:String, pattern:String): String = {
      val localTime = LocalTime.parse(input, DateTimeFormatter.ofPattern(pattern))
      localTime.toString

    }

    def fromTimestampToBqDateString(input: String, pattern:String): String = {
      toUTCZoneDateTime(toZoneDateTime(input, pattern)).toLocalDate.toString
    }
  }


}