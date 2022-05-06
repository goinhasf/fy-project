package services.impl

import dao.users.UserInfo
import data.repositories.EventWizardStateRepository
import scala.concurrent.Future
import endpoints4s.Invalid
import io.circe.JsonObject
import io.circe.parser._
import io.circe.syntax._
import scala.concurrent.ExecutionContext
import dao.events.SocietyEventDetails
import io.circe.Json
import io.circe.Decoder
import io.circe.HCursor
import dao.events.SocietyEvent
import org.bson.types.ObjectId
import dao.forms.FormSubmission
import data.repositories.FormSubmissionsRepository
import dao.events.SocietyEventType
import shared.utils.DateFormatting
import dao.events.Repeat
import java.util.concurrent.TimeUnit
import dao.forms.Submitted
import java.util.Date
import dao.events.Frequency
import data.repositories.EventWizardRepository
import data.repositories.SocietyEventsRepository
import data.repositories.SocietiesRepository
import shared.endpoints.events.wizard.ops.GetEventWizardQuestionState
import shared.endpoints.events.wizard.ops.GetEventWizardState
import play.api.Logger
import data.repositories.FormResourceRepository
import com.google.inject._
import dao.events.QuestionChoice

@Singleton
class EventWizardSubmitServiceImpl @Inject() (
    eventWizardsRepository: EventWizardRepository,
    eventWizardStatesRepository: EventWizardStateRepository,
    formResourcesRepository: FormResourceRepository,
    formSubmissionsRepository: FormSubmissionsRepository,
    societyEventsRepo: SocietyEventsRepository,
    societiesRepo: SocietiesRepository
)(implicit ec: ExecutionContext) {

  val logger = Logger(classOf[EventWizardSubmitServiceImpl])

  def getAllEventTypes() = {
    societyEventsRepo.getSocietyEventTypes()
  }

  def startEventWizard(
      args: (String, String),
      userInfo: UserInfo,
      societyId: String
  ) = {
    val (descId, _) = args
    for {
      desc <- eventWizardsRepository
        .getWizard(descId)
        .collect({ case Some(x) => x })
        .recoverWith({ t: MatchError =>
          Future
            .failed(new Exception(s"Description with $descId not found"))
        })
      id <- eventWizardStatesRepository.createEmptyEventWizardState(
        desc,
        societyId
      )
    } yield (id)
  }

  def getDraftEventWizardStates(societyId: String) = {
    logger.debug(societyId)
    eventWizardStatesRepository.getEventWizardDraftStatesOf(societyId)
  }

  def getEventWizardForEventType(eventType: String) =
    eventWizardsRepository.getWizardForEventType(eventType)

  def submitEventWizardService(
      args: (String, String, String),
      userInfo: UserInfo,
      societyId: String
  ) = {

    val (wizardId, stateId, _) = args

    val future = for {
      eventType <- eventWizardsRepository
        .getWizard(wizardId)
        .collect({ case Some(v) => v })
        .map(_.societyEventType)
      questionStates <- eventWizardStatesRepository
        .getEventWizardStateQuestions(stateId)
      states = questionStates.flatMap(_.selected.toSeq)
    } yield {
      if (states.length != questionStates.length) {
        Future.successful(
          Left(Invalid("Not all questions have been answered"))
        )
      } else {
        val mergedData = states
          .foldRight(JsonObject.empty)((a, b) => a.data.get deepMerge b)
        resolveJsonState(eventType, mergedData, societyId, userInfo) match {
          case Left(value) => Future.successful(Left(value))
          case Right(value) =>
            value
              .flatMap(id =>
                eventWizardStatesRepository
                  .deleteWizardState(stateId)
                  .map(_ => id)
              )
              .map(Right(_))
        }
      }

    }
    future.flatten
  }

  def resolveJsonState(
      eventType: SocietyEventType,
      state: JsonObject,
      societyId: String,
      userInfo: UserInfo
  )(implicit ec: ExecutionContext) = {

    def submitData(
        basicInfo: SocietyEventDetails,
        jsonForms: Seq[(String, JsonObject)]
    ) = {
      for {
        submissions <- Future.sequence(
          jsonForms.map(entry =>
            formSubmissionsRepository.createSubmission(
              FormSubmission(
                new ObjectId().toHexString(),
                entry._1,
                societyId,
                Submitted(userInfo.id, new Date().getTime()),
                entry._2
              )
            )
          )
        )
        societyEvent = SocietyEvent(
          new ObjectId().toHexString(),
          societyId,
          basicInfo,
          submissions.map(_._id)
        )
        id <- societyEventsRepo.insertNewEvent(societyEvent).map(_._id)
      } yield (id)
    }

    val result = for {
      formsJson <- state
        .asJson
        .hcursor
        .downField("forms")
        .as[Map[String, JsonObject]]
        .left
        .map(t =>
          Invalid(
            s"An error occurred decoding 'forms' from state. ${t.message}"
          )
        )
      basicInfo <- state
        .asJson
        .hcursor
        .downField("details")
        .as[SocietyEventDetails](
          EventWizardServiceImpl.customDecoder(eventType)
        )
        .left
        .map(t =>
          Invalid(
            s"An error occurred decoding 'basicInfo' from data state. ${t.message}"
          )
        )
    } yield {
      Right(submitData(basicInfo, formsJson.toSeq))
    }
    result.flatten
  }

  def getEventWizardState(
      args: (String, String),
      user: UserInfo,
      socId: String
  ) = {

    val (eventWizardId, stateWizardId) = args

    val maybeFuture = for {
      wizard <- eventWizardsRepository
        .getWizard(eventWizardId)
        .collect({ case Some(value) => value })
      state <- eventWizardStatesRepository
        .getEventWizardState(stateWizardId)
        .collect({ case Some(value) => value })
      questions <- Future.sequence(
        state.questionStateIds.keySet.map(eventWizardsRepository.getQuestion)
      )
      questionStates <- Future.sequence(
        wizard
          .questionIds
          .map(id =>
            eventWizardStatesRepository
              .getEventWizardStateQuestionBy(state._id, id)
              .collect({ case Some(x) => x })
          )
      )
      s <- getOverallState(user, stateWizardId, socId)
    } yield {

      val sortedQuestionsAndStates = wizard
        .questionIds
        .foldLeft(Seq[GetEventWizardQuestionState]())((a, b) =>
          a :+ GetEventWizardQuestionState(
            questions
              .find(_._id == b)
              .get,
            questionStates.find(_._questionId == b).get.selected,
            s
          )
        )

      Some(GetEventWizardState(state, sortedQuestionsAndStates))
    }
    maybeFuture.recover({
      case t: NoSuchElementException => {
        logger.info("Error occurred in collection of items")
        None
      }
    })
  }

  def getEventWizardQuestionState(
      wizardId: String,
      stateId: String,
      questionId: String,
      user: UserInfo,
      socId: String
  ) = {

    val maybeFuture = for {
      question <- eventWizardsRepository.getQuestion(questionId)
      questionState <- eventWizardStatesRepository
        .getEventWizardStateQuestionBy(
          stateId,
          questionId
        )
        .collect({ case Some(v) => v })
      s <- getOverallState(user, stateId, socId)
    } yield Some(
      GetEventWizardQuestionState(question, questionState.selected, s)
    )

    maybeFuture.recover({
      case t: NoSuchElementException => {
        logger.info("Error occurred in collection of items")
        None
      }
      case other: Throwable => {
        logger.error("Fatal error occurred", other)
        throw other
      }
    })

  }

  def saveQuestionChoice(
      stateId: String,
      questionId: String,
      choice: String
  ) = {
    eventWizardStatesRepository
      .updateQuestionChoice(stateId, questionId, choice)
      .map(_.map(_ => ()))
  }

  def saveEventWizardQuestionState(
      stateId: String,
      questionId: String,
      choice: QuestionChoice
  ) = eventWizardStatesRepository
    .updateQuestionState(stateId, questionId, choice)
    .map(_.map(_ => ()))

  def deleteEventWizardState(stateId: String) =
    eventWizardStatesRepository
      .deleteWizardState(stateId)
      .map(_.map(_ => ()))

  private def getOverallState(
      userInfo: UserInfo,
      stateId: String,
      socId: String
  ) = {

    for {
      society <- societiesRepo
        .getSociety(socId)
        .collect({ case Some(v) => v })
      state <- eventWizardStatesRepository
        .getEventWizardState(stateId)
        .collect({ case Some(value) => value })
        .flatMap(state =>
          Future
            .sequence(
              state
                .questionStateIds
                .keySet
                .toSeq
                .map(id =>
                  eventWizardStatesRepository
                    .getEventWizardStateQuestionBy(stateId, id)
                    .collect({ case Some(value) => value })
                )
            )
            .map(_.flatMap(_.selected.flatMap(_.data).toSeq))
        )
        .map(_.foldRight(JsonObject.empty)(_ deepMerge _))
    } yield (state.deepMerge(
      JsonObject(
        "details" ->
          JsonObject(
            "society"  -> society.details.asJson,
            "userInfo" -> userInfo.asJson
          ).asJson
      )
    ))

  }

}
object EventWizardServiceImpl {
  def customDecoder(eventType: SocietyEventType) =
    new Decoder[SocietyEventDetails] {
      def apply(c: HCursor): Decoder.Result[SocietyEventDetails] = for {
        name        <- c.get[String]("event_name")
        startDate   <- c.get[String]("start_date")
        endDate     <- c.get[String]("end_date")
        startTime   <- c.get[String]("start_time")
        endTime     <- c.get[String]("end_time")
        description <- c.get[String]("description")
        repeats     <- c.get[Int]("repeats")
        frequency   <- c.get[String]("repeat_frequency_unit")
      } yield SocietyEventDetails(
        name,
        eventType,
        DateFormatting.convertStringToDate(startDate, startTime).getTime(),
        DateFormatting.convertStringToDate(endDate, endTime).getTime(),
        description,
        None,
        if (frequency == "None") None
        else
          Some(
            Repeat(repeats.toLong, Frequency(frequency))
          )
      )

    }

}
