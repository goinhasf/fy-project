package dao

import io.circe.generic.extras.Configuration._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.ConfiguredJsonCodec
import io.circe.generic.JsonCodec

@ConfiguredJsonCodec
sealed trait Ownership
object Ownership {
  implicit val configured = Configuration.default.withDiscriminator("_t")
}
@JsonCodec
case class PublicOwnership() extends Ownership
@JsonCodec
case class PrivateOwnership(ownerId: String) extends Ownership
