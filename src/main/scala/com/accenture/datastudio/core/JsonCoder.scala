package com.accenture.datastudio.core

import java.io.{InputStream, OutputStream}

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import org.apache.beam.sdk.coders.{CoderException, CustomCoder, StringUtf8Coder}

class JsonNodeCoder extends CustomCoder[JsonNode]{

  val STRING_UTF_8_CODER = StringUtf8Coder.of()
  val MAPPER = new ObjectMapper()

  override def encode(value: JsonNode, outStream: OutputStream): Unit = {
    if(value == null){
      throw new CoderException("The JsonNodeCoder cannot encode a null object!")
    }
    STRING_UTF_8_CODER.encode(MAPPER.writer().writeValueAsString(value), outStream)

  }

  override def decode(inStream: InputStream): JsonNode = {
    val jsonString = STRING_UTF_8_CODER.decode(inStream)
    MAPPER.reader().readTree(jsonString)
  }
}

object JsonNodeCoder{
  def of: JsonNodeCoder = new JsonNodeCoder()
}
