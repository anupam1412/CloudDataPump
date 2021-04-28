package com.accenture.datastudio.module

import java.lang
import java.nio.file.{Files, Paths}

import com.google.api.services.bigquery.model.TableRow
import com.google.common.collect.ImmutableMap
import com.accenture.datastudio.template.XmlStreamingPipeline
import com.accenture.datastudio.core.BasePipeline
import org.apache.beam.sdk.io.gcp.pubsub.{PubsubMessage, PubsubMessageWithAttributesCoder}
import org.apache.beam.sdk.testing.PAssert
import org.apache.beam.sdk.transforms.Create
import org.apache.beam.sdk.values.TimestampedValue
import org.joda.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test
import org.scalatest.junit.{AssertionsForJUnit, JUnitSuite}

class XmlStreamingPipelineSpec extends JUnitSuite with AssertionsForJUnit with BasePipeline {

  implicit val config = getClass.getResource("/confs/streaming/xml/dataflow.conf").getPath.toValueProvider


  @Test def apptE2E(): Unit = {

    val message = new PubsubMessage(Files.readAllBytes(Paths.get(getClass.getResource("/data/streaming/xml/appointments.xml").toURI)),
      ImmutableMap.of("MessageId", "messsage-id-001-23323", "TopicName", "appointments","LogicalDate", "2019-12-30"))

    val result = XmlStreamingPipeline.transforms(1,1)(p.apply("Create Input",
      Create.timestamped(TimestampedValue.of(message, Instant.now())).withCoder(PubsubMessageWithAttributesCoder.of())))

    PAssert.that(result).satisfies((input: lang.Iterable[TableRow]) => {
      val record: TableRow = input.iterator().next()
      assertEquals(record.get("Type"),"AppointmentBooked")
      assertEquals(record.get("MessageId"), "messsage-id-001"  )
      assertEquals(record.get("TopicName"), "appointments"  )
      assertEquals(record.get("LogicalDate"), "2019-12-30"  )
      null
    })
    p.run

  }
}
