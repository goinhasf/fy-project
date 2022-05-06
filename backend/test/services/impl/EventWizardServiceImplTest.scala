package services.impl

import org.scalatest.funspec.AnyFunSpec
import data.repositories.EventWizardStateRepository
import data.repositories.FormSubmissionsRepository
import org.scalamock.scalatest.MockFactory
import org.scalamock.matchers.Matchers
import org.scalatest.funspec.AsyncFunSpec
import java.util.concurrent.TimeUnit
import dao.events.Frequency
import io.circe.Decoder
import io.circe.HCursor
import io.circe.parser._
import io.circe.syntax._
import dao.events.SocietyEventDetails
import dao.events.SocietyEventType
import shared.utils.DateFormatting
import dao.events.Repeat
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import dao.events.EventWizardQuestionState
import dao.events.QuestionChoice
import dao.forms.FormSubmission
import io.circe.JsonObject
import dao.users.UserInfo
import shared.auth.Role
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import shared.auth.Privilege
import io.circe.Json
import dao.forms.Submitted
import data.repositories.EventWizardRepository
import dao.events.EventWizardDescriptor
import data.repositories.SocietyEventsRepository
import dao.events.SocietyEvent
import data.repositories.FormResourceRepository
import data.repositories.SocietiesRepository
import dao.events.EventWizardQuestion
import dao.societies.Society
import dao.societies.SocietyDetails
import dao.events.EventWizardState
import shared.endpoints.events.wizard.ops.GetEventWizardQuestionState

class EventWizardServiceImplTest
    extends AnyFunSpec
    with MockFactory
    with Matchers {

  val details = s"""
    {
      "details": {
        "repeats" : 1,
        "description" : "Nothin",
        "end_time" : "13:00",
        "start_time" : "12:00",
        "end_date" : "2021-10-10",
        "start_date" : "2021-10-10",
        "event_name" : "Event A",
        "repeat_frequency_unit" : "Weekly"
      }
    }
  """

  val formData = s"""
    {
      "forms": {
        "form_id": {
          "value_a": "abc",
          "value_b": "def"
        }
      }
    }
  """

  val jsonObject = s"""
      {
        $formData,
        $details
      }
  """

  describe("parsing") {
    it(
      "should correctly parse the society details using the custom parser data"
    ) {
      parse(details)
        .map(_.hcursor.downField("details"))
        .flatMap(_.as[Json])
        .map(_.hcursor)
        .flatMap(cursor =>
          EventWizardServiceImpl
            .customDecoder(SocietyEventType("Social"))
            .apply(cursor)
        ) match {
        case Left(value) => {
          println(value.fillInStackTrace())
          fail(value)
        }
        case Right(value) => {
          assert(true)
        }
      }

      parse(formData) match {
        case Left(value)  => fail("Could not parse form data", value)
        case Right(value) => assert(true)
      }
      parse(details).flatMap(_.as[JsonObject]) match {
        case Left(value)  => fail("Could not parse details", value)
        case Right(value) => assert(true)
      }

    }

    val eventWizardsRepository: EventWizardRepository =
      mock[EventWizardRepository]
    val eventWizardStatesRepository: EventWizardStateRepository =
      mock[EventWizardStateRepository]
    val formResourcesRepository: FormResourceRepository =
      mock[FormResourceRepository]
    val formSubmissionsRepository: FormSubmissionsRepository =
      mock[FormSubmissionsRepository]
    val societyEventsRepo: SocietyEventsRepository =
      mock[SocietyEventsRepository]
    val societiesRepo: SocietiesRepository = mock[SocietiesRepository]

    val service = new EventWizardSubmitServiceImpl(
      eventWizardsRepository,
      eventWizardStatesRepository,
      formResourcesRepository,
      formSubmissionsRepository,
      societyEventsRepo,
      societiesRepo
    )

    val questionStates = Seq(
      EventWizardQuestionState(
        "1",
        "a",
        "x",
        Some(
          QuestionChoice(
            "Social",
            parse(details).flatMap(_.as[JsonObject]).toOption
          )
        )
      ),
      EventWizardQuestionState(
        "2",
        "a",
        "x",
        Some(
          QuestionChoice(
            "Yes",
            parse(formData).flatMap(_.as[JsonObject]).toOption
          )
        )
      )
    )

    val society = Society(
      "societyId",
      SocietyDetails("Some society", None, None, None, None),
      None
    )

    val userInfo = UserInfo(
      "user_id",
      "test",
      "tester",
      "test@tester.com",
      Role.RegularUser(
        Set(Privilege.CommitteeResourceManagement("society_id"))
      )
    )

    it("should successfully submit the event data") {

      (eventWizardStatesRepository.getEventWizardStateQuestions _)
        .expects(*)
        .returns(Future.successful(questionStates))

      (eventWizardStatesRepository.deleteWizardState _)
        .expects(*)
        .returns(Future.successful(Some(1)))

      (formSubmissionsRepository.createSubmission _)
        .expects(*)
        .once()
        .returns(
          Future.successful(
            FormSubmission(
              "fid",
              "resourceId",
              "socId",
              Submitted("userId", System.currentTimeMillis()),
              questionStates.last.selected.get.data.get
            )
          )
        )

      (eventWizardsRepository.getWizard _)
        .expects(*)
        .returns(
          Future.successful(
            Some(
              EventWizardDescriptor(
                "abc",
                SocietyEventType("Social")
              )
            )
          )
        )

      val societyEventMock =
        SocietyEvent("abc", "abc", mock[SocietyEventDetails], Seq())

      (societyEventsRepo.insertNewEvent _)
        .expects(*)
        .returns(Future.successful(societyEventMock))

      Await.result(
        service.submitEventWizardService(
          ("a", "x", "any"),
          UserInfo(
            "user_id",
            "test",
            "tester",
            "test@tester.com",
            Role.RegularUser(
              Set(Privilege.CommitteeResourceManagement("society_id"))
            )
          ),
          "society_id"
        ),
        Duration.Inf
      ) match {
        case Left(value)  => fail(s"Future failed with: $value")
        case Right(value) => assert(true)
      }

    }

    it(
      "should prevent the user from submitting wizard without necessary information"
    ) {
      val questionStates = Seq(
        EventWizardQuestionState(
          "1",
          "a",
          "x",
          Some(
            QuestionChoice(
              "Social",
              parse("{}").flatMap(_.as[JsonObject]).toOption
            )
          )
        ),
        EventWizardQuestionState(
          "2",
          "a",
          "x",
          Some(
            QuestionChoice(
              "Yes",
              parse(formData).flatMap(_.as[JsonObject]).toOption
            )
          )
        )
      )
      (eventWizardsRepository.getWizard _)
        .expects(*)
        .returns(
          Future.successful(
            Some(
              EventWizardDescriptor(
                "abc",
                SocietyEventType("Social")
              )
            )
          )
        )

      (eventWizardStatesRepository.getEventWizardStateQuestions _)
        .expects(*)
        .returns(Future.successful(questionStates))

      val societyEventMock =
        SocietyEvent("abc", "abc", mock[SocietyEventDetails], Seq())

      Await.result(
        service.submitEventWizardService(
          ("a", "x", "any"),
          userInfo,
          "society_id"
        ),
        Duration.Inf
      ) match {
        case Left(value)  => assert(true)
        case Right(value) => fail(s"Future failed with: $value")
      }

    }

    it("should get the question state in the correct format") {

      val eventWizardDescriptor = EventWizardDescriptor(
        "abc",
        SocietyEventType("Social")
      )

      val question = EventWizardQuestion("qId", "title", Map())
      val choice =
        QuestionChoice("Yes", Some(JsonObject("hello" -> "there".asJson)))
      val questionState = EventWizardQuestionState(
        "qsId",
        question._id,
        "abc",
        Some(choice)
      )

      val eventWizardState = EventWizardState(
        "wizardStateId",
        eventWizardDescriptor._id,
        society._id,
        Map(question._id -> questionState._id)
      )

      (eventWizardsRepository
        .getQuestion(_: String))
        .expects(*)
        .returns(Future.successful(question))

      (eventWizardStatesRepository.getEventWizardStateQuestionBy _)
        .expects(*, *)
        .twice()
        .returns(Future.successful(Some(questionState)))

      (societiesRepo.getSociety _)
        .expects(*)
        .returns(Future.successful(Some(society)))

      (eventWizardStatesRepository.getEventWizardState _)
        .expects(*)
        .returns(Future.successful(Some(eventWizardState)))

      val result = Await.result(
        service.getEventWizardQuestionState(
          eventWizardDescriptor._id,
          eventWizardState._id,
          question._id,
          userInfo,
          society._id
        ),
        Duration.Inf
      )

      result match {
        case Some(value) =>
          assertResult(
            value
          )(
            GetEventWizardQuestionState(
              question,
              Some(choice),
              choice
                .data
                .get
                .deepMerge(
                  JsonObject(
                    "details" -> JsonObject(
                      "society"  -> society.details.asJson,
                      "userInfo" -> userInfo.asJson
                    ).asJson
                  )
                )
            )
          )
        case None =>
      }

    }
    it("should save the event wizard progress") {}
    it("should get the wizard state in the correct format") {}

  }

}
