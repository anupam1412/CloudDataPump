package com.accenture.datastudio.module

import java.lang

import com.accenture.datastudio.core.{BasePipeline, JsonNodeCoder}
import com.accenture.datastudio.template.AvroBatchPipeline
import com.fasterxml.jackson.databind.JsonNode
import com.spotify.scio.bigquery.TableRow
import org.apache.beam.sdk.testing.PAssert
import org.junit.Assert.assertEquals
import org.junit.Test
import org.scalatest.junit.{AssertionsForJUnit, JUnitSuite}


class AvroBatchPipelineSpec extends JUnitSuite with  BasePipeline with AssertionsForJUnit{


  @Test def avro_template_transforms(): Unit = {

    implicit val config = getClass.getResource("/confs/batch/avro/dataflow.conf").getPath.toValueProvider

    p.getCoderRegistry.registerCoderForClass(classOf[JsonNode], JsonNodeCoder.of)

    val result = AvroBatchPipeline.transforms(nullValueProvider)
    PAssert.that(result).satisfies((input: lang.Iterable[TableRow]) => {
      val tr = input.iterator().next()
      assertEquals(tr.get("type"),"AppointmentBooked")
      assertEquals(tr.get("appointmentid"), "6c1dadd8-a71a-457a-a8d1-570033a5931a")
      assertEquals(tr.get("timestamputc"), "2017-06-17:09:37Z")
      assertEquals(tr.get("discipline"), "physio")
      //assertEquals(tr.get("pdate"), "2019-07-16")
      assertEquals(tr.get("bq_row_id").asInstanceOf[String].nonEmpty, true)
      assertEquals(tr.get("bq_row_src").asInstanceOf[String].contains("src/test/resources/data/batch/avro/appointments.avro"), true)
      null
    })
    p.run()

  }
}

