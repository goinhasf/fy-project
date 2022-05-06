package services.impl

import com.google.inject._
import data.repositories.SocietyEventsRepository
import scala.concurrent.Future
import dao.events.SocietyEvent
import org.mongodb.scala._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Updates._
import io.circe.parser._
import io.circe.syntax._
import shared.endpoints.events.wizard.SocietyEventsEndpoints
import shared.endpoints.events.wizard.SocietyEventsEndpoints.SubmittedQ
import shared.endpoints.events.wizard.SocietyEventsEndpoints.UnderReviewQ
import shared.endpoints.events.wizard.SocietyEventsEndpoints.ReviewedQ
import shared.endpoints.events.wizard.SocietyEventsEndpoints.AnyQ
import shared.endpoints.events.wizard.GetSocietyEvent
import dao.forms.FormResource
import dao.forms.FormSubmission
import scala.concurrent.ExecutionContext
import dao.users.UserInfo
import shared.auth.RegularUserRole
import dao.societies.Society
import dao.events.Reviewed
import shared.endpoints.ChangeSubmissionStatus
import shared.endpoints.ChangeToReviewed
import dao.forms.Ready
import dao.forms.Submitted
import dao.forms.InReview
import dao.forms
import dao.events.SocietyEventStatus

@Singleton
class SocietyEventsService(
    societyEventsCollection: MongoCollection[Document],
    formSubmissionService: FormSubmissionsService,
    societiesCollection: MongoCollection[Document]
) {
  def getSocietyEvents(
      userInfo: UserInfo,
      societyId: Option[String],
      status: SocietyEventsEndpoints.SocietyEventStatusQ
  )(implicit ec: ExecutionContext): Future[Seq[GetSocietyEvent]] = {

    val filter = status match {
      case SubmittedQ   => equal("societyEventStatus._t", "Submitted")
      case UnderReviewQ => equal("societyEventStatus._t", "UnderReview")
      case ReviewedQ    => equal("societyEventStatus._t", "Reviewed")
      case AnyQ         => empty()
    }

    val userControlFilter = if (userInfo.role.roleType == RegularUserRole()) {
      and(equal("_societyId", societyId.get), filter)
    } else {
      filter
    }

    societyEventsCollection
      .find(userControlFilter)
      .collect()
      .map(_.map(_.toJson()))
      .map(_.map(decode[SocietyEvent](_).fold(throw _, identity)))
      .map(_.map(_._id))
      .head()
      .flatMap(seq =>
        Future.sequence(
          seq.map(id =>
            getSocietyEvent(id, userInfo, societyId).collect({
              case Some(value) => value
            })
          )
        )
      )
  }

  private def getSociety(id: String) = societiesCollection
    .find(equal("_id", id))
    .map(_.toJson())
    .map(decode[Society](_).fold(throw _, identity))
    .head()

  def getSocietyEvent(
      id: String,
      userInfo: UserInfo,
      societyId: Option[String]
  )(implicit ec: ExecutionContext): Future[Option[GetSocietyEvent]] = {

    def formSubmissions(ids: Seq[String]) =
      Future.sequence(
        ids.map(id =>
          formSubmissionService
            .getFormSubmission(id, userInfo, societyId)
            .collect({ case Some(value) => value })
        )
      )

    val filter = if (userInfo.role.roleType == RegularUserRole()) {
      and(equal("_societyId", societyId.get), equal("_id", id))
    } else {
      equal("_id", id)
    }

    societyEventsCollection
      .find(filter)
      .map(_.toJson())
      .map(decode[SocietyEvent](_).fold(throw _, identity))
      .headOption()
      .flatMap(_ match {
        case Some(event) =>
          formSubmissions(event.formSubmissionIds)
            .flatMap(seq =>
              getSociety(event._societyId)
                .map(soc => Some(GetSocietyEvent(event, seq, soc)))
            )

        case None => Future.successful(None)
      })
  }

  def reviewEvent(
      args: (String, SocietyEventStatus, String),
      userInfo: UserInfo,
      societyId: Option[String]
  )(implicit ec: ExecutionContext): Future[String] = {

    if (userInfo.role.roleType == RegularUserRole()) {
      Future.failed(
        new IllegalAccessException(
          "User does not have the privileges to access this page"
        )
      )
    } else {

      val (id, status, _) = args

      getSocietyEvent(id, userInfo, societyId)
        .collect({ case Some(value) => value })
        .map(_.formSubmissions)
        .flatMap(seq =>
          Future.sequence(
            seq
              .filter(_.submission.formSubmissionStatus match {
                case r: forms.Reviewed => false
                case _                 => true
              })
              .map(form =>
                formSubmissionService.changeFormSubmissionStatus(
                  form.submission._id,
                  ChangeToReviewed(userInfo.id, false, None),
                  userInfo,
                  ""
                )
              )
          )
        )
        .flatMap(_ =>
          societyEventsCollection
            .updateOne(
              equal("_id", id),
              set(
                "societyEventStatus",
                Document(status.asJson.noSpaces)
              )
            )
            .map(_ => id)
            .head()
        )

    }

  }

}
