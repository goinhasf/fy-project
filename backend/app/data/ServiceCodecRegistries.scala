package data

import registries._
import org.bson.codecs.configuration.CodecRegistries.{
  fromProviders,
  fromRegistries
}
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY

object ServiceCodecRegistries {
  def apply() = fromRegistries(
    FormResourceRegistry(),
    SocietyRegistry(),
    DEFAULT_CODEC_REGISTRY
  )
}
