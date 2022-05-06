package data.repositories

import scala.concurrent.Future
import dao.events.EventWizardQuestion
import org.mongodb.scala._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Updates._
import dao.events.EventWizardDescriptor
import com.mongodb.client.model.InsertManyOptions
import io.circe.parser._
import io.circe.syntax._
import dao.events.SocietyEventType

trait EventWizardRepository {
  def getQuestion(id: String): Future[EventWizardQuestion]
  def getQuestion(id: String, index: Int): Future[(EventWizardQuestion, Int)]
  def getWizardForEventType(
      eventType: String
  ): Future[Option[EventWizardDescriptor]]
  def getWizard(wizardId: String): Future[Option[EventWizardDescriptor]]
  def insertQuestions(
      questions: Seq[EventWizardQuestion]
  ): Future[Long]
  def createWizard(
      wizardDesc: EventWizardDescriptor
  ): Future[EventWizardDescriptor]

  def mapQuestionToWizard(
      wizardId: String,
      questions: EventWizardQuestion
  ): Future[EventWizardDescriptor]
}

class EventWizardRepositoryImpl(
    wizardDescriptorCollection: MongoCollection[Document],
    questionsCollection: MongoCollection[Document]
) extends EventWizardRepository {

  override def getWizardForEventType(
      eventType: String
  ): Future[Option[EventWizardDescriptor]] =
    wizardDescriptorCollection
      .find(
        equal(
          "societyEventType.name",
          eventType
        )
      )
      .map(_.toJson())
      .map(decode[EventWizardDescriptor](_).fold(throw _, identity))
      .headOption()

  override def getQuestion(id: String): Future[EventWizardQuestion] =
    questionsCollection
      .find(equal("_id", id))
      .map(_.toJson())
      .map(decode[EventWizardQuestion](_).fold(throw _, identity))
      .head()

  override def getQuestion(
      id: String,
      index: Int
  ): Future[(EventWizardQuestion, Int)] = questionsCollection
    .find(equal("_id", id))
    .map(_.toJson())
    .map(decode[EventWizardQuestion](_).fold(throw _, x => (x, index)))
    .head()

  override def insertQuestions(
      questions: Seq[EventWizardQuestion]
  ): Future[Long] = questionsCollection
    .insertMany(questions.map(_.asJson.noSpaces).map(Document.apply))
    .map(_.getInsertedIds().size().toLong)
    .head()

  override def createWizard(
      wizardDesc: EventWizardDescriptor
  ): Future[EventWizardDescriptor] = wizardDescriptorCollection
    .insertOne(Document(wizardDesc.asJson.noSpaces))
    .map(_ => wizardDesc)
    .head()

  override def mapQuestionToWizard(
      wizardId: String,
      question: EventWizardQuestion
  ): Future[EventWizardDescriptor] = {

    questionsCollection
      .find(equal("_id", question._id))
      .map(_.toJson())
      .map(decode[EventWizardQuestion](_).fold(throw _, identity))
      .flatMap(q =>
        wizardDescriptorCollection.findOneAndUpdate(
          equal("_id", wizardId),
          addToSet("questionsIds", q._id)
        )
      )
      .map(_.toJson())
      .map(decode[EventWizardDescriptor](_).fold(throw _, identity))
      .head()

  }

  override def getWizard(
      wizardId: String
  ): Future[Option[EventWizardDescriptor]] = wizardDescriptorCollection
    .find(equal("_id", wizardId))
    .map(_.toJson())
    .map(decode[EventWizardDescriptor](_).fold(throw _, identity))
    .headOption()

}
