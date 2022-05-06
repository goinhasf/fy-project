package data.repositories

import org.mongodb.scala.MongoCollection
import org.bson.types.ObjectId
import scala.concurrent.Future
import org.mongodb.scala.model.Filters
import dao.PrivateOwnership
import dao.PublicOwnership
import dao.forms.FormResourceDetails
import dao.forms.FormResource
import com.mongodb.client.result.InsertOneResult
import scala.concurrent.ExecutionContext
import dao.users.UserInfo
import io.circe.Json
import io.circe.JsonObject
import org.mongodb.scala._
import org.mongodb.scala.model.Updates._
import dao.forms.FormResourceFields
import io.circe.parser._
import io.circe.syntax._
import dao.forms.FormResourceFieldDescriptor
import org.mongodb.scala.model.FindOneAndUpdateOptions
import com.mongodb.client.model.ReturnDocument
import dao.Ownership

class FormResourceRepository(collection: MongoCollection[Document]) {

  def findResource(id: String): Future[Option[FormResource]] = collection
    .find(Filters.equal("_id", id))
    .map(_.toJson())
    .map(decode[FormResource](_).fold(throw _, identity))
    .headOption()

  def findAllResources(userId: String): Future[Seq[FormResource]] = {

    val resourcesBelongingToUser = Filters
      .equal(
        "resourceOwnership",
        Document(
          Ownership.codecForOwnership.apply(PrivateOwnership(userId)).noSpaces
        )
      )

    val allPublicResources =
      Filters.equal(
        "resourceOwnership",
        Document(Ownership.codecForOwnership.apply(PublicOwnership()).noSpaces)
      )

    collection
      .find(Filters.or(resourcesBelongingToUser, allPublicResources))
      .collect()
      .map { seq =>
        seq
          .map(_.toJson())
          .map(decode[FormResource](_).fold(throw _, identity))
      }
      .head()
  }
  def insertResourceFull(
      formResource: FormResource
  ): Future[InsertOneResult] = collection
    .insertOne(Document(formResource.asJson.noSpaces))
    .head()

  def insertResource(
      formResourceDetails: FormResourceDetails,
      userDetails: UserInfo,
      formResourceFields: Seq[FormResourceFieldDescriptor]
  )(implicit ec: ExecutionContext): Future[FormResource] = {
    val formResource = FormResource(
      new ObjectId().toHexString(),
      formResourceDetails,
      Option
        .when(formResourceDetails.isPublic)(PublicOwnership())
        .getOrElse(PrivateOwnership(userDetails.id)),
      formResourceFields
    )
    collection
      .insertOne(Document(formResource.asJson.noSpaces))
      .head()
      .map(_ => formResource)
  }

  def updateFormResourceDetails(
      id: String,
      details: FormResourceDetails
  ): Future[Option[FormResource]] = {
    collection
      .findOneAndUpdate(
        Filters.eq("_id", id),
        set("details", Document(details.asJson.noSpaces)),
        FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
      )
      .map(_.toJson())
      .map(decode[FormResource](_).fold(throw _, identity))
      .headOption()
  }

  def updateFormResourceDefaultFieldValues(
      id: String,
      values: JsonObject
  ): Future[Option[FormResource]] = {
    collection
      .findOneAndUpdate(
        Filters.equal("_id", id),
        set("defaultFieldValues", Document(Some(values).asJson.noSpaces)),
        FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
      )
      .map(_.toJson())
      .map(decode[FormResource](_).fold(throw _, identity))
      .headOption()
  }
}
