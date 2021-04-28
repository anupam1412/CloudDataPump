package com.accenture.datastudio.transformers.helpers

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

import com.fasterxml.jackson.databind.JsonNode
import com.google.api.services.bigquery.model.TableRow
import org.apache.beam.sdk.coders.Coder.Context
import org.apache.beam.sdk.io.gcp.bigquery.TableRowJsonCoder

import scala.util.{Failure, Success, Try}

trait TableRowWriter[T]{
  def writer(input: T):TableRow
}

object TableRowWriterInstance{

  implicit val stringWriter = new TableRowWriter[String] {
    override def writer(input: String): TableRow = {
      Try(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))) match {
        case Success(is) => TableRowJsonCoder.of().decode(is, Context.OUTER)
        case Failure(exception) => throw exception
      }
    }
  }

  implicit val jsonNodeWriter = new TableRowWriter[JsonNode] {
    override def writer(input: JsonNode): TableRow = {
      Try(new ByteArrayInputStream(input.toString.getBytes(StandardCharsets.UTF_8))) match {
        case Success(is) => TableRowJsonCoder.of().decode(is, Context.OUTER)
        case Failure(exception) => throw exception
      }
    }
  }

}

object TableRowSyntax{
  implicit class tableRowOps[A](input: A) {
    def toTableRow(implicit w: TableRowWriter[A]): TableRow = {
      w.writer(input)
    }

  }
}