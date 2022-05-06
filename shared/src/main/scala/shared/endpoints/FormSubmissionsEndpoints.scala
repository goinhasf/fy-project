package shared.endpoints
import endpoints4s.algebra
import io.circe.syntax._
import dao.forms.FormSubmission
import dao.forms.FormResource
import io.circe.generic.JsonCodec
import dao.forms.FormSubmissionStatus
import dao.forms.StatusType
import dao.societies.Society
trait FormSubmissionsEndpoints
    extends algebra.Endpoints
    with algebra.circe.JsonEntitiesFromCodecs
    with SecurityExtensions {

  def restPath      = path / "api" / "form-submissions"
  def adminRestPath = path / "admin" / "api" / "form-submissions"

  def getFormSubmission = endpoint(
    get(restPath / segment[String]("id")),
    ok(jsonResponse[Option[GetFormSubmission]])
  )

  def getSubmittedFormSubmissions = endpoint(
    get(adminRestPath / "submitted"),
    ok(jsonResponse[Seq[GetFormSubmission]])
  )

  def getInReviewFormSubmissions = endpoint(
    get(adminRestPath / "in-review"),
    ok(jsonResponse[Seq[GetFormSubmission]])
  )

  def getReviewedFormSubmissions = endpoint(
    get(adminRestPath / "reviewed"),
    ok(jsonResponse[Seq[GetFormSubmission]])
  )

  def changeFormSubmissionStatus = endpoint(
    csrfPut(
      adminRestPath / segment[String]("id"),
      jsonRequest[ChangeSubmissionStatus],
      emptyRequestHeaders
    ),
    ok(jsonResponse[GetFormSubmission]).orNotFound()
  )

  def getUserName = endpoint(
    get(restPath / "user-name" / segment[String]("id")),
    ok(textResponse)
  )

}

@JsonCodec
case class GetFormSubmission(
    formResource: FormResource,
    submission: FormSubmission,
    society: Society
)

@JsonCodec
sealed trait ChangeSubmissionStatus
case class ChangeToReady()                   extends ChangeSubmissionStatus
case class ChangeToSubmitted(userId: String) extends ChangeSubmissionStatus
case class ChangeToInReview(userId: String)  extends ChangeSubmissionStatus
case class ChangeToReviewed(
    reviewerId: String,
    requestChanges: Boolean,
    notes: Option[String]
) extends ChangeSubmissionStatus
