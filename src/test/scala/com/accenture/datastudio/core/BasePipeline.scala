package com.accenture.datastudio.core

import com.accenture.datastudio.core.templates.PipelineBuilderOptions
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import org.apache.beam.sdk.options.PipelineOptionsFactory
import org.apache.beam.sdk.options.ValueProvider.NestedValueProvider
import org.apache.beam.sdk.testing.TestPipeline
import org.junit.Rule

trait BasePipeline {
  val options = PipelineOptionsFactory.create().as(classOf[PipelineBuilderOptions])
  val _pipeline = TestPipeline.fromOptions(options)

  val mapper = new ObjectMapper()


  @Rule
  implicit def p = _pipeline
  val coderegistry = p.getCoderRegistry
  coderegistry.registerCoderForClass(classOf[JsonNode], JsonNodeCoder.of)

  val nullValueProvider =  NestedValueProvider.of[String, String]("".toValueProvider, (str:String) => null)


  implicit class ValueProvider[T](value: T){
    def toValueProvider = p.newProvider[T](value)
  }

}
