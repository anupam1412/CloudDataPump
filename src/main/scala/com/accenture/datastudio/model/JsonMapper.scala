package com.accenture.datastudio.model

import scala.reflect.ClassTag


trait JsonMapper[T] {

  def apply(json: String)(implicit t:ClassTag[T]): T = {
    toJsonObject[T](json , t.runtimeClass.asInstanceOf[Class[T]])
  }

}