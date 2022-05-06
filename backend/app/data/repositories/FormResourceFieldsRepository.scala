package data.repositories

import scala.concurrent.Future
import dao.forms.FormResourceFields
import io.circe.Json
import io.circe.syntax._
import io.circe.parser._
import org.mongodb.scala._
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Updates._
import io.circe.JsonObject
import dao.forms.FormResourceFieldDescriptor

trait FormResourceFieldsRepository {
  def findFormFieldsByResourceId(
      resourceId: String,
  ): Future[Option[FormResourceFields]]

  def createNewFormResourceFieldsRecord(
      formResourceFields: FormResourceFields
  ): Future[FormResourceFields]
}

class FormResourceFieldsRepositoryImpl(
    collection: MongoCollection[Document]
) extends FormResourceFieldsRepository {

  def findFormFieldsByResourceId(
      resourceId: String,
  ): Future[Option[FormResourceFields]] = collection
    .find(
      equal("_formResourceId", resourceId)
    )
    .map(document =>
      decode[FormResourceFields](document.toJson()) match {
        case Left(value)  => throw value
        case Right(value) => value
      }
    )
    .headOption()

  def createNewFormResourceFieldsRecord(
      formResourceFields: FormResourceFields
  ): Future[FormResourceFields] = collection
    .insertOne(Document(formResourceFields.asJson.noSpaces))
    .map(_ => formResourceFields)
    .head()

}
