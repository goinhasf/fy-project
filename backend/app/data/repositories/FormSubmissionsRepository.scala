package data.repositories

import org.mongodb.scala._
import scala.concurrent.Future
import dao.forms.FormSubmission
import io.circe.syntax._

trait FormSubmissionsRepository {
  def createSubmission(formSubmission: FormSubmission): Future[FormSubmission]
}

class FormSubmissionsRepositoryImpl(
    collection: MongoCollection[Document]
) extends FormSubmissionsRepository {
  def createSubmission(
      formSubmission: FormSubmission
  ): Future[FormSubmission] = {
    collection
      .insertOne(Document(formSubmission.asJson.noSpaces))
      .map(_ => formSubmission)
      .head()
  }
}
