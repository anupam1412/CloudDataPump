package com.accenture.datastudio.model


import com.fasterxml.jackson.annotation.JsonProperty
import com.spotify.scio.bigquery.types.BigQueryType
import scala.collection.JavaConverters._

object AppointmentDetail{
  @BigQueryType.toTable
  case class AppointmentDetails(@JsonProperty("AppointmentId") AppointmentId : String,
                                @JsonProperty("TimestampUtc")  TimestampUtc : String,
                                @JsonProperty("Disciplines")  Disciplines : List[String])

  object AppointmentDetails extends JsonMapper[AppointmentDetails]{

  }
}

