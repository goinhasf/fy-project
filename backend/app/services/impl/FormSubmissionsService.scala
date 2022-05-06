package services.impl

import org.mongodb.scala._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Updates._
import scala.concurrent.Future
import dao.forms.FormSubmission
import io.circe.parser._
import io.circe.syntax._
import com.google.inject
import dao.users.UserInfo
import shared.endpoints.GetFormSubmission
import data.repositories.FormResourceRepository
import scala.concurrent.ExecutionContext
import play.api.Logger
import shared.auth.GuildAdminRole
import shared.auth.AdminRole
import dao.forms.FormSubmissionStatus
import dao.forms.StatusType
import dao.forms.ReadyType
import dao.forms.SubmittedType
import dao.forms.InReviewType
import dao.forms.ReviewedType
import dao.forms.Ready
import dao.forms.Submitted
import dao.forms.InReview
import dao.forms.Reviewed
import java.util.Date
import shared.endpoints.ChangeSubmissionStatus
import shared.endpoints.ChangeToReady
import shared.endpoints.ChangeToSubmitted
import shared.endpoints.ChangeToInReview
import shared.endpoints.ChangeToReviewed
import dao.societies.Society

class FormSubmissionsService(
    formResourcesRepo: FormResourceRepository,
    formSubmissionsServiceCollection: MongoCollection[Document],
    societiesCollection: MongoCollection[Document]
) {

  val logger = Logger(classOf[FormSubmissionsService])

  private def isUserAdmin(userInfo: UserInfo) = {
    if (
      userInfo.role.roleType == AdminRole() || userInfo
        .role
        .roleType == GuildAdminRole()
    ) {
      true
    } else {
      false
    }
  }

  private def isAuthorized(
      userInfo: UserInfo,
      formSub: FormSubmission,
      societyId: String
  ): Boolean = {

    isUserAdmin(userInfo) || formSub._societyId == societyId

  }

  private def getSociety(sub: FormSubmission) = societiesCollection
    .find(equal("_id", sub._societyId))
    .first()
    .map(_.toJson())
    .map(decode[Society](_).fold(throw _, identity))
    .head()

  def getFormSubmission(
      id: String,
      userInfo: UserInfo,
      societyId: Option[String]
  )(implicit ec: ExecutionContext): Future[Option[GetFormSubmission]] = {

    formSubmissionsServiceCollection
      .find(
        equal("_id", id)
      )
      .map(_.toJson())
      .map(decode[FormSubmission](_).fold(throw _, identity))
      .headOption()
      .collect({ case Some(v) => v })
      .filter(sub => isAuthorized(userInfo, sub, societyId.getOrElse("")))
      .flatMap(sub => getSociety(sub).map(soc => (sub, soc)))
      .flatMap(args =>
        formResourcesRepo
          .findResource(args._1._formResourceId)
          .collect({ case Some(value) =>
            Some(GetFormSubmission(value, args._1, args._2))
          })
          .recover({
            case t: Throwable => {
              logger.error("Error occurred obtaining form resource")
              throw t
            }
          })
      )
      .recover({
        case t: NoSuchElementException => {
          logger
            .info(
              s"Illegal access from user #${userInfo.id} to submission occurred",
              t
            )
          throw new IllegalAccessException(
            "User does not have the right privileges to access this resource"
          )
        }
        case t: Throwable => None
      })

  }

  def getAllFormSubmissions(
      statuses: Seq[String],
      userInfo: UserInfo,
      societyId: String
  )(implicit ec: ExecutionContext): Future[Seq[GetFormSubmission]] = {
    if (
      userInfo.role.roleType == AdminRole() || userInfo
        .role
        .roleType == GuildAdminRole()
    ) {

      println(statuses)

      val filter = Document(
        "$and" -> statuses.map(status =>
          Document(
            "$or" -> Seq(
              Document(
                "formSubmissionStatus._t" -> status
              )
            )
          )
        )
      )

      formSubmissionsServiceCollection
        .find(filter)
        .collect()
        .map(
          _.map(_.toJson())
            .map(decode[FormSubmission](_).fold(throw _, identity))
        )
        .head()
        .flatMap(seq =>
          Future.sequence(
            seq.map(submission =>
              formResourcesRepo
                .findResource(submission._formResourceId)
                .collect({ case Some(value) => value })
                .flatMap(resource =>
                  getSociety(submission)
                    .map(soc => GetFormSubmission(resource, submission, soc))
                )
                .recover({
                  case t: Throwable => {
                    logger.error("Error occurred obtaining form resource")
                    throw t
                  }
                })
            )
          )
        )

    } else {
      logger
        .info(
          s"Illegal access from user #${userInfo.id} to submission occurred"
        )
      Future.failed {
        throw new IllegalAccessException(
          "User does not have the right privileges to access this resource"
        )
      }
    }

  }

  def changeFormSubmissionStatus(
      id: String,
      newStatus: ChangeSubmissionStatus,
      userInfo: UserInfo,
      societyId: String
  )(implicit ec: ExecutionContext) = {
    formSubmissionsServiceCollection
      .find(equal("_id", id))
      .first()
      .map(_.toJson())
      .map(decode[FormSubmission](_).fold(throw _, identity))
      .filter(sub => isAuthorized(userInfo, sub, societyId))
      .flatMap { sub =>
        formSubmissionsServiceCollection
          .updateOne(
            equal("_id", id),
            set(
              "formSubmissionStatus",
              Document(
                createNewStatus(newStatus, sub.formSubmissionStatus, userInfo)
                  .asJson
                  .noSpaces
              )
            )
          )
      }
      .head()
      .flatMap(_ => getFormSubmission(id, userInfo, Some(societyId)))
      .recover({
        case ill: IllegalAccessException => {
          logger.error(
            "System should not be wrongly switching form submission statuses",
            ill
          )
          throw ill
        }
        case _: NoSuchElementException => {
          logger
            .info(
              s"Illegal access from user #${userInfo.id} to submission occurred"
            )
          throw new IllegalAccessException(
            "User does not have the right privileges to access this resource"
          )
        }
        case t: Throwable => None
      })
  }

  private def createNewStatus(
      statusType: ChangeSubmissionStatus,
      prevStatus: FormSubmissionStatus,
      userInfo: UserInfo
  ): FormSubmissionStatus = statusType match {
    case ChangeToReady() => Ready()
    case ChangeToSubmitted(userId) =>
      prevStatus match {
        case Ready()      => Submitted(userId, new Date().getTime())
        case s: Submitted => s
        case InReview(
              _submittedById,
              _reviewerId,
              submissionDate,
              reviewStartDate
            ) =>
          Submitted(_submittedById, submissionDate)
        case Reviewed(
              _submittedById,
              _reviewerId,
              submissionDate,
              reviewStartDate,
              reviewEndDate,
              changesRequested,
              notes
            ) =>
          Submitted(_submittedById, submissionDate)
      }
    case ChangeToInReview(userId) =>
      prevStatus match {
        case Ready() =>
          throw new IllegalArgumentException(
            "Can not move status from ready to InReview"
          )
        case Submitted(_submittedById, submissionDate) => {
          if (isUserAdmin(userInfo)) {
            InReview(
              _submittedById,
              userId,
              submissionDate,
              new Date().getTime()
            )
          } else {
            throw new IllegalAccessException(
              "Regular user is not allowed to change status to in review"
            )
          }
        }
        case in: InReview => in
        case Reviewed(
              _submittedById,
              _reviewerId,
              submissionDate,
              reviewStartDate,
              reviewEndDate,
              changesRequested,
              notes
            ) =>
          InReview(_submittedById, _reviewerId, submissionDate, reviewStartDate)
      }
    case ChangeToReviewed(reviewerId, requestChanges, notes) =>
      prevStatus match {
        case Ready() =>
          throw new IllegalArgumentException(
            "Can not move status from ready to Reviewed"
          )
        case Submitted(_submittedById, submissionDate) => {
          if (isUserAdmin(userInfo)) {
            Reviewed(
              _submittedById,
              userInfo.id,
              submissionDate,
              new Date().getTime(),
              new Date().getTime(),
              false,
              None
            )
          } else {
            throw new IllegalAccessException(
              "Regular user is not allowed to change status to in review"
            )
          }
        }
        case InReview(
              _submittedById,
              _reviewerId,
              submissionDate,
              reviewStartDate
            ) => {
          throw new IllegalArgumentException(
            "Can not move status from Reviewed to InReview"
          )
        }
        case r: Reviewed => r
      }
  }

}
