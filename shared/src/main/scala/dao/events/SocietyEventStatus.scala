package dao.events

import io.circe.generic.extras.ConfiguredJsonCodec
import io.circe.generic.JsonCodec

@ConfiguredJsonCodec
sealed trait SocietyEventStatus {
  val status: String
}

case class UnderReview() extends SocietyEventStatus {
  val status: String = "Under review"
}

case class Submitted() extends SocietyEventStatus {
  val status: String = "Submitted"
}

case class Reviewed(
    notes: String,
    requiresChanges: Boolean
) extends SocietyEventStatus {
  val status: String = "Reviewed"
}
object SocietyEventStatus {
  import io.circe.generic.extras.auto._
  import io.circe.generic.extras.Configuration
  implicit val genDevConfig: Configuration = Configuration
    .default
    .withDiscriminator("_t")
}
