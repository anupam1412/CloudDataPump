package com.accenture.datastudio

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

package object model {

  val objectMapper = new ObjectMapper() with ScalaObjectMapper
  objectMapper.registerModule(DefaultScalaModule)

  def toJsonObject[T](json : String, clazz:Class[T]): T = {
    objectMapper.readValue(json, clazz)
  }

}
