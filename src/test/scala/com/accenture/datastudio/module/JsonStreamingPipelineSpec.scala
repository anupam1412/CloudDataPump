package com.accenture.datastudio.module

import java.lang
import java.nio.file.{Files, Paths}

import com.accenture.datastudio.core.BasePipeline
import com.accenture.datastudio.template.JsonStreamingPipeline
import com.google.api.services.bigquery.model.TableRow
import com.google.common.collect.ImmutableMap
import org.apache.beam.sdk.io.gcp.pubsub.{PubsubMessage, PubsubMessageWithAttributesCoder}
import org.apache.beam.sdk.testing.PAssert
import org.apache.beam.sdk.transforms.Create
import org.apache.beam.sdk.values.TimestampedValue
import org.joda.time.Instant
import org.junit.Assert.{assertEquals, assertFalse, assertNotNull, assertTrue}
import org.junit.{Before, Test}
import org.scalatest.junit.{AssertionsForJUnit, JUnitSuite}

import collection.JavaConverters._
import scala.collection.mutable.{Buffer, Map => MMap}

class JsonStreamingPipelineSpec extends JUnitSuite with AssertionsForJUnit with BasePipeline{

  @Test def JsonPipelineE2E_success(): Unit ={

    implicit val config = getClass.getResource("/confs/streaming/json/dataflow.conf").getPath.toValueProvider

    def toMap(javaMap:Any): MMap[String, Any] = {
      javaMap.asInstanceOf[java.util.Map[String, Any]].asScala
    }

    def toList(javaList:Any): Buffer[java.util.Map[String, Any]] = {
      javaList.asInstanceOf[java.util.List[java.util.Map[String, Any]]].asScala
    }

    val message = new PubsubMessage(Files.readAllBytes(Paths.get(getClass.getResource("/data/streaming/json/appointments.json").toURI)),
      ImmutableMap.of("MessageId", "messsage-id-001", "TopicName", "appointments","LogicalDate", "2019-12-30"))

    val (success, failure) = JsonStreamingPipeline.transforms(p.apply("Create Input",
      Create.timestamped(TimestampedValue.of(message, Instant.now())).withCoder(PubsubMessageWithAttributesCoder.of())))

    PAssert.that(success).satisfies((input: lang.Iterable[TableRow]) => {
      val record: TableRow = input.iterator().next()
      val dataMap = toList(record.get("Data"))
      val dataMap0 = toMap(dataMap(0))
      assertEquals("AppointmentBooked", record.get("Type"))
      assertEquals("2017-06-17:09:37Z", dataMap0("TimestampUtc"))
      assertEquals("6c1dadd8-a71a-457a-a8d1-570033a5931a", dataMap0("AppointmentId"))
      null
    })

p.run


}

}
case class FakeTest(id: String, name:String)

