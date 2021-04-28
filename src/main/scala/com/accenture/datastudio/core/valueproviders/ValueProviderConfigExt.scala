package com.accenture.datastudio.core.valueproviders


import java.nio.channels.Channels

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.beam.sdk.io.FileSystems
import org.apache.beam.sdk.options.ValueProvider
import org.apache.beam.sdk.options.ValueProvider.NestedValueProvider
import org.apache.beam.sdk.util.StreamUtils

trait ValueProviderConfigExt[A,B] {
  def toValueProvider(a: ValueProvider[A],root:Option[A]): ValueProvider[B]
}
object ValueProviderConfigExtProvider  {
  implicit val configValueProvider = new ValueProviderConfigExt[String, Config] {
    override def toValueProvider(a: ValueProvider[String], root: Option[String]): ValueProvider[Config] = {
      NestedValueProvider.of[Config, String](a, (conf: String) => {
        val confStr = Some(conf)
          .map(i => readFileContent(i))
          .map(i => ConfigFactory.parseString(i)).orNull
        root match {
          case Some(c) => confStr.getObject(c).toConfig
          case None => confStr
        }
      })
    }
  }

  def readFileContent(path: String) : String  = {
    val readableByteChannel = FileSystems.open(FileSystems.matchNewResource(path, false))
    val json = new String(
      StreamUtils.getBytesWithoutClosing(Channels.newInputStream(readableByteChannel)))
    json
  }

}
object ValueProviderConfigExtSyntax{

  val errorPath = (config: Config) => config.getString("errorPath")
  val rawBucketPath = (config: Config) => config.getString("messages-bucket")


  implicit class ObjectConfigStringValueProvider(id: String) extends Serializable {
    def toValueProvider(implicit cvp: ValueProviderConfigExt[String,Config],config: ValueProvider[String],root: Option[String]) =
      NestedValueProvider.of[String,Config](cvp.toValueProvider(config,root), (config: Config) => {
        config.getString(id)
      })
  }

  implicit class ObjectConfigFnValueProvider[A](f: Config => A) extends Serializable {
    def toValueProvider(implicit cvp:ValueProviderConfigExt[String,Config],config:ValueProvider[String],root: Option[String]) =
      NestedValueProvider.of[A,Config](cvp.toValueProvider(config,root), (config: Config) => {
        f(config)
      })
  }

}
