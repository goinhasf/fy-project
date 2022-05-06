package dao.forms

import io.circe.generic.extras.ConfiguredJsonCodec
import io.circe.generic.JsonCodec
import io.circe.JsonObject

@JsonCodec
case class FormSubmission(
    _id: String,
    _formResourceId: String,
    _societyId: String,
    formSubmissionStatus: FormSubmissionStatus,
    values: JsonObject
)
@ConfiguredJsonCodec
sealed trait StatusType
case class ReadyType()     extends StatusType
case class SubmittedType() extends StatusType
case class InReviewType()  extends StatusType
case class ReviewedType()  extends StatusType
object StatusType {
  import io.circe.generic.extras.auto._
  import io.circe.generic.extras.Configuration
  implicit lazy val configured = Configuration.default.withDiscriminator("_t")
}

@ConfiguredJsonCodec
sealed trait FormSubmissionStatus
case class Ready() extends FormSubmissionStatus
case class Submitted(_submittedById: String, submissionDate: Long)
    extends FormSubmissionStatus
case class InReview(
    _submittedById: String,
    _reviewerId: String,
    submissionDate: Long,
    reviewStartDate: Long
) extends FormSubmissionStatus
case class Reviewed(
    _submittedById: String,
    _reviewerId: String,
    submissionDate: Long,
    reviewStartDate: Long,
    reviewEndDate: Long,
    changesRequested: Boolean,
    notes: Option[String]
) extends FormSubmissionStatus

object FormSubmissionStatus {
  import io.circe.generic.extras.auto._
  import io.circe.generic.extras.Configuration
  implicit lazy val configured = Configuration.default.withDiscriminator("_t")
}
