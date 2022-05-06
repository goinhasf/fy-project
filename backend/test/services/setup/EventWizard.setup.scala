package services.setup

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import io.circe.syntax._
import org.mongodb.scala._
import org.scalatest.funspec.AsyncFunSpec
import dao.events.EventWizardQuestion
import data.ServiceCodecRegistries
import org.mongodb.scala.model.Filters
import dao.events.EventWizardDescriptor
import dao.forms.FormResource
import org.bson.types.ObjectId
import dao.forms.FormResourceFields
import dao.forms.FormResourceDetails
import dao.forms.FormCategory
import dao.PublicOwnership
import data.repositories.FormResourceFieldsRepositoryImpl
import play.api.libs.ws.WSClient
import io.circe.JsonObject
import scala.concurrent.ExecutionContext
import dao.events.SocietyEventType
import io.circe.Codec
import io.circe.{Decoder, HCursor}
import dao.events.JsonInputQuestionResolver

object EventWizardSetup {

  def apply(
      ticketsFormResourceId: String,
      externalPaymentsFormId: String,
      riskAssessmentFormId: String
  )(implicit ec: ExecutionContext) = {
    val mongoClient = Utils.getMongoClient

    val societyManagementDb = mongoClient
      .getDatabase("society-management-app")
      .withCodecRegistry(ServiceCodecRegistries())

    val eventWizardQuestionsCollection =
      societyManagementDb.getCollection(
        "event-wizard-questions"
      )

    val eventWizardDescriptorsCollection =
      societyManagementDb.getCollection(
        "event-wizards"
      )

    val formResourcesCollection = societyManagementDb
      .getCollection[FormResource]("form-resources")

    val formResourceFieldsRepo = new FormResourceFieldsRepositoryImpl(
      societyManagementDb
        .getCollection("form-resource-fields")
    )

    val eventTypesCollection = societyManagementDb.getCollection("event-types")

    import dao.forms.FormResourceFieldDescriptor.codec

    val questions = GenericEventWizardQuestions(
      ticketsFormResourceId,
      externalPaymentsFormId,
      riskAssessmentFormId
    )

    val genericEventWizard = GenericEventWizard(questions)

    for {
      _ <- eventTypesCollection.deleteMany(Filters.empty()).head()
      _ <- eventTypesCollection
        .deleteMany(Filters.empty())
        .flatMap(_ =>
          eventTypesCollection
            .insertOne(
              Document(SocietyEventType("Social").asJson.noSpaces)
            )
        )
        .head()
      _ <- eventWizardQuestionsCollection
        .deleteMany(Filters.empty())
        .head()
      _ <- eventWizardDescriptorsCollection
        .deleteMany(Filters.empty())
        .head()
      questionInsertion <- eventWizardQuestionsCollection
        .insertMany(
          questions
            .map(_.asJson.noSpaces)
            .map(Document.apply)
        )
        .head()
      descriptorInsertion <- eventWizardDescriptorsCollection
        .insertOne(
          Document(
            genericEventWizard.asJson.noSpaces
          )
        )
        .head()
    } yield ()

  }
}
