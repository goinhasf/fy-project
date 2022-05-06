package data.registries

import org.bson.codecs.configuration.CodecProvider
import org.bson.codecs.Codec
import org.bson.codecs.configuration.CodecRegistry
import io.circe.Json
import org.mongodb.scala.bson._
import org.mongodb.scala.bson.codecs.Macros._
import org.bson.codecs.configuration.CodecRegistries.fromProviders
import org.bson.BsonReader
import org.bson.codecs.DecoderContext
import org.bson.BsonWriter
import org.bson.codecs.EncoderContext
import io.circe.parser.{decode => decodeJson}
import io.circe.syntax._
import io.circe._
import dao.forms.FormResource
import dao.forms.FormResourceDetails
import dao.forms.FormCategory
import dao.Ownership
import dao.PrivateOwnership
import dao.PublicOwnership
import dao.forms.FormResourceFields

object FormResourceRegistry {

  def apply() = fromProviders(
    classOf[FormResource],
    createCodecProvider[FormResourceDetails](),
    createCodecProvider[FormCategory](),
    createCodecProvider[Ownership](),
    createCodecProvider[PrivateOwnership](),
    createCodecProvider[PublicOwnership](),
    classOf[FormResourceFields]
  )

}
