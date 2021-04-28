package com.accenture.datastudio.model


import com.accenture.datastudio.model.AppointmentDetail.AppointmentDetails
import com.fasterxml.jackson.annotation.JsonProperty
import com.spotify.scio.bigquery.types.BigQueryType

object Appointment{
  @BigQueryType.toTable
  case class Appointments(@JsonProperty("Type") Type: String,
                          @JsonProperty Data : List[AppointmentDetails],
                         @JsonProperty TopicName : String,
                         @JsonProperty MessageId : String,
                         @JsonProperty LogicalDate : String)

  object Appointments extends JsonMapper[Appointments]{


  }

}
