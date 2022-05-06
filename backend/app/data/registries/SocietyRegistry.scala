package data.registries
import org.bson.codecs.configuration.CodecRegistries.{
  fromProviders,
  fromRegistries
}
import org.mongodb.scala.bson.codecs.Macros
import dao.societies.Society
import dao.societies.SocietyDetails

object SocietyRegistry {
  def apply() = fromProviders(
    Macros.createCodecProvider[SocietyDetails](),
    Macros.createCodecProvider[Society]()
  )
}
