package dao.societies

import io.circe.generic.semiauto._

case class Society(
    _id: String,
    details: SocietyDetails,
    pictureFileId: Option[String]
)

case class SocietyDetails(
    name: String,
    description: Option[String],
    publicEmailAddress: Option[String],
    webPageUrl: Option[String],
    facebookUrl: Option[String]
)
object SocietyDetails {
  implicit val codec = deriveCodec[SocietyDetails]
}

object Society {
  implicit val codec = deriveCodec[Society]
}
