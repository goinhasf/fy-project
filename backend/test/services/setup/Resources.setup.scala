package services.setup

import clients.play.FormResourcesClient
import clients.play.auth.AuthenticationClient
import clients.play.auth.config.AuthenticationClientConfig
import com.typesafe.config.ConfigFactory
import dao.PublicOwnership
import dao.forms.FormCategory
import dao.forms.FormResource
import dao.forms.FormResourceDetails
import io.circe.syntax._
import org.bson.types.ObjectId
import org.scalatest.AsyncTestSuite
import org.scalatest.funspec.AsyncFunSpec
import play.api.http.ContentTypes
import play.api.http.Port
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.ControllerComponents
import play.api.mvc.DefaultControllerComponents
import play.api.test.WsTestClient
import play.core.server.NettyServer
import shared.auth.ApiKey
import shared.auth.UserCredentialsAuth
import shared.config.JwtConfig
import shared.config.MicroServiceConfig
import shared.endpoints.FormResourceEndpoints
import utils.MicroServiceConfigLoader

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import io.circe.Json
import akka.stream.scaladsl.Source
import play.api.mvc.MultipartFormData
import org.mongodb.scala.model.Filters

object FormResourceObjects {

  val ticketFormResource = FormResourceDetails(
    "Tickets Form",
    "Fill this form if your event requires the guild to issue tickets",
    List(FormCategory("Tickets")),
    true,
    Some("b16d0d2e-b4c4-4037-881b-5ed4df66b804")
  )

  val externalPaymentsForm = FormResourceDetails(
    "External Payments Form",
    "Perform a payment to an external entity",
    List(FormCategory("Finance")),
    true,
    Some("03a18945-b1c5-4ad7-9d0f-f43e93f5fbfb")
  )

  val riskAssessmentForm = FormResourceDetails(
    "Prepared Risk Assessment - General/Small Scale Socials",
    "A risk assessment for small scale events",
    List(FormCategory("Risk Assessment")),
    true,
    Some("dc9f616b-2c25-441f-9d17-bece7c1b9e6e")
  )
}

class ResourcesSetup extends AsyncFunSpec {

  import FormResourceObjects._

  val config      = MicroServiceConfigLoader.load(ConfigFactory.load())
  implicit val ec = ExecutionContext.Implicits.global

  val customConfig = new MicroServiceConfig {
    val apiKey: ApiKey       = config.apiKey
    val jwtConfig: JwtConfig = config.jwtConfig
    val serviceUrl: String   = "http://localhost:8080"
  }

  val authClientConfig = new AuthenticationClientConfig {
    val apiKey: ApiKey       = config.apiKey
    val jwtConfig: JwtConfig = config.jwtConfig
    val serviceUrl: String   = "http://localhost:8080"
    val cookieName: String   = "smSID"
  }

  implicit val port = new Port(9100)

  val app = GuiceApplicationBuilder().build()

  WsTestClient.withClient { client =>
    val resourceClient = new FormResourcesClient(
      customConfig,
      client,
      app.injector.instanceOf[DefaultControllerComponents]
    )

    val authorizationClient = new AuthenticationClient(
      authClientConfig,
      client,
      app.injector.instanceOf[DefaultControllerComponents]
    )

    val credentials =
      UserCredentialsAuth("test@tester.com", "password").asJson.noSpaces

    Await.result(
      for {
        _ <- Utils
          .getMongoClient
          .getDatabase("society-management-app")
          .getCollection("form-resources")
          .deleteMany(Filters.empty())
          .head()
        token <- client
          .url("http://localhost:8080/api/login")
          .withHttpHeaders(
            "Content-Type" -> ContentTypes.JSON,
            "Csrf-Token"   -> "nocheck"
          )
          .post(credentials)
          .map(_.body)
        ticketR <- resourceClient.insertResourceWithExistingFile(
          (ticketFormResource, "nocheck", s"Bearer $token")
        )
        extR <- resourceClient.insertResourceWithExistingFile(
          (externalPaymentsForm, "nocheck", s"Bearer $token")
        )
        riskR <- resourceClient.insertResourceWithExistingFile(
          (riskAssessmentForm, "nocheck", s"Bearer $token")
        )
        _ <- EventWizardSetup.apply(ticketR._id, extR._id, riskR._id)

      } yield (),
      Duration.Inf
    )

  }

}
