package shared.endpoints.events.wizard
import endpoints4s.algebra
import shared.endpoints.SecurityExtensions
import dao.events.SocietyEvent
import dao.events.SocietyEventStatus
import dao.events.Submitted
import dao.events.UnderReview
import dao.events.Reviewed
import shared.endpoints.events.wizard.SocietyEventsEndpoints.SubmittedQ
import shared.endpoints.events.wizard.SocietyEventsEndpoints.UnderReviewQ
import shared.endpoints.events.wizard.SocietyEventsEndpoints.ReviewedQ
import shared.endpoints.events.wizard.SocietyEventsEndpoints.AnyQ
import dao.forms.FormSubmission
import dao.forms.FormResource
import io.circe.generic.JsonCodec
import shared.endpoints.GetFormSubmission
import dao.societies.Society

trait SocietyEventsEndpoints
    extends algebra.Endpoints
    with algebra.circe.JsonEntitiesFromCodecs
    with SecurityExtensions {

  def restPath = path / "api" / "events"

  implicit val statusQ
      : QueryStringParam[SocietyEventsEndpoints.SocietyEventStatusQ] =
    stringQueryString.xmap(_ match {
      case "submitted" =>
        SubmittedQ.asInstanceOf[SocietyEventsEndpoints.SocietyEventStatusQ]
      case "review" =>
        UnderReviewQ.asInstanceOf[SocietyEventsEndpoints.SocietyEventStatusQ]
      case "reviewed" =>
        ReviewedQ.asInstanceOf[SocietyEventsEndpoints.SocietyEventStatusQ]
      case _ => AnyQ.asInstanceOf[SocietyEventsEndpoints.SocietyEventStatusQ]
    })(_ match {
      case SocietyEventsEndpoints.SubmittedQ   => "submitted"
      case SocietyEventsEndpoints.UnderReviewQ => "review"
      case SocietyEventsEndpoints.ReviewedQ    => "reviewed"
      case SocietyEventsEndpoints.AnyQ         => "any"
    })

  def getSocietyEvents = endpoint(
    get(
      restPath /? qs[SocietyEventsEndpoints.SocietyEventStatusQ]("eventType")
    ),
    ok(jsonResponse[Seq[GetSocietyEvent]])
  )

  def getSocietyEventDetails = endpoint(
    get(
      restPath / segment[String]("id")
    ),
    ok(jsonResponse[GetSocietyEvent]).orNotFound()
  )

  def reviewEvent = endpoint(
    csrfPut(
      restPath / segment[String]("id"),
      jsonRequest[SocietyEventStatus],
      emptyRequestHeaders
    ),
    ok(textResponse)
  )
}

@JsonCodec
case class GetSocietyEvent(
    event: SocietyEvent,
    formSubmissions: Seq[GetFormSubmission],
    society: Society
)

object SocietyEventsEndpoints {
  sealed trait SocietyEventStatusQ
  case object SubmittedQ   extends SocietyEventStatusQ
  case object UnderReviewQ extends SocietyEventStatusQ
  case object ReviewedQ    extends SocietyEventStatusQ
  case object AnyQ         extends SocietyEventStatusQ
}
