package com.accenture.datastudio.module

import java.lang

import com.accenture.datastudio.core.BasePipeline
import com.google.api.services.bigquery.model.TableRow
import com.accenture.datastudio.template.CSVBatchPipeline
import org.apache.beam.sdk.testing.PAssert._
import org.junit.Assert.assertEquals
import org.junit.Test
import org.scalatest.junit.{AssertionsForJUnit, JUnitSuite}
import com.accenture.datastudio.core.templates.{stringFailedTag, tableRowSuccessTag}

import scala.collection.JavaConverters._


class CSVBatchPipelineSpec extends JUnitSuite with AssertionsForJUnit with BasePipeline{



  @Test def csv_transforms(): Unit = {
    implicit val config = getClass.getResource("/confs/batch/csv/dataflow.conf").getPath.toValueProvider


    val result = CSVBatchPipeline.transforms(nullValueProvider).get(tableRowSuccessTag)

    that(result).satisfies((input: lang.Iterable[TableRow]) => {
      val record = input.iterator().asScala.toIterable.toList
      assertEquals(record.size, 1)
      record.foreach {
        tr => {
          assertEquals(tr.size(), 6)
          assertEquals(tr.get("Type"),"AppointmentBooked")
          assertEquals(tr.get("AppointmentId"), "6c1dadd8-a71a-457a-a8d1-570033a5931a")
          assertEquals(tr.get("TimestampUTC"), "2017-06-17:09:37Z")
          assertEquals(tr.get("Discipline"), "physio")
          assertEquals(tr.get("bq_row_id").asInstanceOf[String].nonEmpty, true)
          assertEquals(tr.get("bq_row_src").asInstanceOf[String].contains("src/test/resources/data/batch/csv/appointments.csv"), true)
        }
      }
      null
    })
    p.run

  }

  @Test def csv_transforms_with_partition(): Unit = {
    implicit val config = getClass.getResource("/confs/batch/csv/dataflow_with_partitioning.conf").getPath.toValueProvider


    val result = CSVBatchPipeline.transforms(nullValueProvider).get(tableRowSuccessTag)

    that(result).satisfies((input: lang.Iterable[TableRow]) => {
      val record = input.iterator().asScala.toIterable.toList
      assertEquals(record.size, 1)
      record.foreach {
        tr => {
          assertEquals(tr.size(), 7)
          assertEquals(tr.get("Type"),"AppointmentBooked")
          assertEquals(tr.get("AppointmentId"), "6c1dadd8-a71a-457a-a8d1-570033a5931a")
          assertEquals(tr.get("TimestampUTC"), "2017-06-17:09:37Z")
          assertEquals(tr.get("Discipline"), "physio")
          assertEquals(tr.get("Transactional_Date"), "2019-12-28")
          assertEquals(tr.get("bq_row_id").asInstanceOf[String].nonEmpty, true)
          assertEquals(tr.get("bq_row_src").asInstanceOf[String].contains("src/test/resources/data/batch/csv/20191228/appointments_20191228.csv"), true)
        }
      }
      null
    })
    p.run

  }

}
