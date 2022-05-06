package data.repositories

import scala.concurrent.Future
import dao.events.EventWizardState
import dao.events.EventWizardQuestionState
import dao.events.QuestionChoice
import org.mongodb.scala._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Updates._
import org.bson.types.ObjectId
import io.circe.syntax._
import io.circe.parser._
import dao.events.EventWizardDescriptor

trait EventWizardStateRepository {
  def createEmptyEventWizardState(
      desc: EventWizardDescriptor,
      societyId: String
  ): Future[String]
  def getEventWizardDraftStatesOf(
      societyId: String
  ): Future[Seq[EventWizardState]]
  def getEventWizardState(id: String): Future[Option[EventWizardState]]
  def getEventWizardStateQuestions(
      stateWizardId: String
  ): Future[Seq[EventWizardQuestionState]]
  def getEventWizardStateQuestionBy(
      stateId: String,
      questionId: String
  ): Future[Option[EventWizardQuestionState]]
  def updateQuestionState(
      eventWizardStateId: String,
      questionId: String,
      questionState: QuestionChoice
  ): Future[Option[EventWizardQuestionState]]
  def updateQuestionChoice(
      eventWizardStateId: String,
      questionId: String,
      questionChoice: String
  ): Future[Option[QuestionChoice]]
  def createQuestionState(
      questionId: String,
      wizardStateId: String,
      selected: QuestionChoice
  ): Future[EventWizardQuestionState]
  def deleteWizardState(
      stateId: String
  ): Future[Option[Long]]
}

class EventWizardStateRepositoryImpl(
    eventWizardStateCollection: MongoCollection[Document],
    questionsStateCollection: MongoCollection[Document]
) extends EventWizardStateRepository {
  def createEmptyEventWizardState(
      desc: EventWizardDescriptor,
      societyId: String
  ): Future[String] = {

    def emptyQuestion(qId: String, stateId: String) = EventWizardQuestionState(
      new ObjectId().toHexString(),
      qId,
      stateId,
      None
    )

    val emptyState = EventWizardState(
      new ObjectId().toHexString(),
      desc._id,
      societyId
    )

    eventWizardStateCollection
      .insertOne(Document(emptyState.asJson.noSpaces))
      .map(_ => emptyState)
      .flatMap { state =>
        val questionToStateSeq = desc
          .questionIds
          .map(qid => (qid, emptyQuestion(qid, state._id)))

        val questionStates = questionToStateSeq
          .map(_._2)

        val questionStateIds = questionToStateSeq
          .map(entry => (entry._1, entry._2._id))
          .toMap

        questionsStateCollection
          .insertMany(
            questionStates.map(q => Document(q.asJson.noSpaces))
          )
          .flatMap(_ =>
            eventWizardStateCollection.updateOne(
              equal("_id", state._id),
              set(
                "questionStateIds",
                questionStateIds
              )
            )
          )
          .map(_ => state._id)
      }
      .head()

  }

  def getEventWizardDraftStatesOf(
      societyId: String
  ): Future[Seq[EventWizardState]] = eventWizardStateCollection
    .find(equal("_societyId", societyId))
    .collect()
    .map(
      _.map(_.toJson()).map(decode[EventWizardState](_).fold(throw _, identity))
    )
    .head()

  def getEventWizardStateQuestions(
      stateWizardId: String
  ): Future[Seq[EventWizardQuestionState]] = questionsStateCollection
    .find(equal("_eventWizardStateId", stateWizardId))
    .collect()
    .map(
      _.map(_.toJson())
        .map(decode[EventWizardQuestionState](_).fold(throw _, identity))
    )
    .head()

  def getEventWizardState(id: String): Future[Option[EventWizardState]] =
    eventWizardStateCollection
      .find(equal("_id", id))
      .map(_.toJson())
      .map(decode[EventWizardState](_).fold(throw _, identity))
      .headOption()

  def getEventWizardStateQuestionBy(
      stateId: String,
      questionId: String
  ): Future[Option[EventWizardQuestionState]] = questionsStateCollection
    .find(
      and(
        equal("_eventWizardStateId", stateId),
        equal("_questionId", questionId)
      )
    )
    .map(_.toJson())
    .map(decode[EventWizardQuestionState](_).fold(throw _, identity))
    .headOption()

  def updateQuestionState(
      eventWizardStateId: String,
      questionId: String,
      questionState: QuestionChoice
  ): Future[Option[EventWizardQuestionState]] = {

    val findFilter = and(
      equal("_eventWizardStateId", eventWizardStateId),
      equal("_questionId", questionId)
    )

    questionsStateCollection
      .updateOne(
        findFilter,
        set("selected", Document(Some(questionState).asJson.noSpaces))
      )
      .flatMap(_ => questionsStateCollection.find(findFilter))
      .map(_.toJson())
      .map(decode[EventWizardQuestionState](_).fold(throw _, identity))
      .headOption()
  }

  def updateQuestionChoice(
      eventWizardStateId: String,
      questionId: String,
      questionChoice: String
  ): Future[Option[QuestionChoice]] = {
    val findFilter = and(
      equal("_eventWizardStateId", eventWizardStateId),
      equal("_questionId", questionId)
    )
    questionsStateCollection
      .find(findFilter)
      .map(_.toJson())
      .map(decode[EventWizardQuestionState](_).fold(throw _, identity))
      .flatMap { state =>
        val choice = state.selected match {
          case Some(value) => QuestionChoice(questionChoice, value.data)
          case None        => QuestionChoice(questionChoice, None)
        }
        questionsStateCollection
          .updateOne(
            findFilter,
            set("selected", Document(choice.asJson.noSpaces))
          ).map(_ => choice)
      }
      .headOption()
  }

  def createQuestionState(
      questionId: String,
      wizardStateId: String,
      selected: QuestionChoice
  ): Future[EventWizardQuestionState] = {

    val value = EventWizardQuestionState(
      new ObjectId().toHexString(),
      questionId,
      wizardStateId,
      Some(selected)
    )

    questionsStateCollection
      .insertOne(Document(value.asJson.toString()))
      .map(_ => value)
      .head()
  }

  def deleteWizardState(stateId: String): Future[Option[Long]] =
    eventWizardStateCollection
      .deleteOne(equal("_id", stateId))
      .flatMap(_ =>
        questionsStateCollection
          .deleteMany(equal("_eventWizardStateId", stateId))
      )
      .map(_.getDeletedCount())
      .headOption()
}
