package data.repositories

import scala.concurrent.Future

import com.google.inject._
import dao.societies.Society
import dao.societies.SocietyDetails
import dao.users.UserInfo
import org.bson.types.ObjectId
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Updates._
import org.mongodb.scala.SingleObservable
import org.mongodb.scala.Observable

trait SocietiesRepository {
  def insertSociety(society: Society): Future[Society]
  def updateSocietyDetails(
      id: String,
      details: SocietyDetails
  ): Future[Option[Society]]
  def getSociety(id: String): Future[Option[Society]]
  def getSocietyWithName(name: String): Future[Option[Society]]
}
@Singleton
class SocietiesRepositoryImpl(collection: MongoCollection[Society])
    extends SocietiesRepository {

  override def insertSociety(society: Society): Future[Society] = {
    collection
      .insertOne(society)
      .map(_ => society)
      .head()
  }

  override def updateSocietyDetails(
      id: String,
      details: SocietyDetails
  ): Future[Option[Society]] = {
    collection
      .findOneAndUpdate(
        equal("_id", id),
        set("details", details)
      )
      .headOption()
  }

  override def getSociety(id: String): Future[Option[Society]] = {
    collection
      .find(equal("_id", id))
      .headOption()
  }

  def getSocietyWithName(name: String): Future[Option[Society]] = collection
    .find(equal("details.name", name))
    .headOption()

}
