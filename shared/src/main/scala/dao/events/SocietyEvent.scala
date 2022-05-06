package dao.events

import io.circe.generic.JsonCodec
import io.circe.generic.semiauto._
import java.util.concurrent.TimeUnit
import io.circe.Codec
import io.circe.{Decoder, HCursor}
import io.circe.Json
import io.circe.syntax._
import io.circe.generic.extras.ConfiguredJsonCodec
import java.text.DateFormat
import java.util.Date
import java.time.LocalDate
import shared.utils.DateFormatting._

@JsonCodec
case class SocietyEvent(
    _id: String,
    _societyId: String,
    details: SocietyEventDetails,
    formSubmissionIds: Seq[String],
    societyEventStatus: SocietyEventStatus = Submitted(),
    facebookEventId: Option[String] = None
)

@JsonCodec
case class SocietyEventDetails(
    name: String,
    eventType: SocietyEventType,
    startDateTime: Long,
    endDateTime: Long,
    description: String,
    location: Option[String] = None,
    repeats: Option[Repeat] = None
)
object SocietyEventDetails {}

@JsonCodec
case class Frequency(value: String)
object Frequency {
  val Yearly  = Frequency("Yearly")
  val Monthly = Frequency("Monthly")
  val Weekly  = Frequency("Weekly")
}

@JsonCodec
case class Repeat(value: Long, timeUnit: Frequency)
object Repeat {
  implicit val timeUnitCodec = new Codec[Frequency] {
    def apply(c: HCursor): Decoder.Result[Frequency] = for {
      timeUnit <- c.downField("timeUnit").as[String]
    } yield Frequency(timeUnit)

    def apply(a: Frequency): Json =
      Json.obj("timeUnit" -> Json.fromString(a.value))
  }
}
