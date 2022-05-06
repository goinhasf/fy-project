package data.repositories

import scala.concurrent.Future
import dao.events.SocietyEvent
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.model.Filters._
import dao.events.SocietyEventType
import org.mongodb.scala._
import io.circe.parser._
import io.circe.syntax._

trait SocietyEventsRepository {
  def findEventById(id: String): Future[Option[SocietyEvent]]
  def insertNewEvent(event: SocietyEvent): Future[SocietyEvent]
  def updateEvent(
      oldEvent: SocietyEvent,
      newEvent: SocietyEvent
  ): Future[SocietyEvent]
  def getSocietyEventTypes(): Future[Seq[SocietyEventType]]
}

class SocietyEventsRepositoryImpl(
    collection: MongoCollection[Document],
    eventTypesCollection: MongoCollection[Document]
) extends SocietyEventsRepository {

  

  override def findEventById(id: String): Future[Option[SocietyEvent]] =
    collection
      .find(equal("_id", id))
      .map(_.toJson())
      .map(decode[SocietyEvent](_).fold(throw _, identity))
      .headOption()

  override def insertNewEvent(event: SocietyEvent): Future[SocietyEvent] =
    collection
      .insertOne(Document(event.asJson.noSpaces))
      .map(_ => event)
      .head()

  override def updateEvent(
      oldEvent: SocietyEvent,
      newEvent: SocietyEvent
  ): Future[SocietyEvent] = collection
    .replaceOne(equal("_id", oldEvent._id), Document(newEvent.asJson.noSpaces))
    .map(_ => newEvent)
    .head()

  override def getSocietyEventTypes(): Future[Seq[SocietyEventType]] =
    eventTypesCollection
      .find()
      .collect()
      .map(
        _.map(_.toJson())
          .map(decode[SocietyEventType](_).fold(throw _, identity))
      )
      .head()

}
