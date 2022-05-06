package services.endpoints

import org.scalatest.funspec.AnyFunSpec
import org.scalamock.scalatest.MockFactory
import org.scalamock.proxy.ProxyMockFactory
import play.api.inject.guice.GuiceApplicationBuilder
import shared.config.MicroServiceConfig
import play.api.inject.bind
import shared.config.JwtConfig
import shared.auth.ApiKey
import play.api.Mode
import data.repositories.FormResourceRepository
import clients.play.FileUploadClient
import clients.play.DocumentAnalyserClient
import clients.play.auth.AuthenticationClient
import play.api.test.WsTestClient
import play.api.mvc.ControllerComponents
import clients.play.auth.config.AuthenticationClientConfig
import play.test.WSTestClient
import services.impl.FormResourcesService
import dao.forms.FormResourceDetails
import dao.forms.FormCategory
import dao.users.UserInfo
import shared.auth.Role
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import io.FileMetadata
import play.api.mvc.MultipartFormData
import io.circe.syntax._
import play.api.libs.Files
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import akka.stream.scaladsl.Source
import dao.forms.FormResource
import dao.PublicOwnership
import play.api.libs.ws.WSClient
import akka.util.ByteString
import dao.forms.FormResourceFieldDescriptor
import endpoints4s.Invalid
import scala.concurrent.ExecutionContext

class FormResourceUploadTest extends AnyFunSpec with MockFactory {

  val port = 9100
  val microServicesConfig = new MicroServiceConfig {
    override val serviceUrl: String   = s"http://localhost:$port"
    override val jwtConfig: JwtConfig = mock[JwtConfig]
    override val apiKey: ApiKey       = ApiKey("test", "abcd")
  }
  val authenticationServiceConfig = new AuthenticationClientConfig {
    override val serviceUrl: String   = s"http://localhost:$port"
    override val jwtConfig: JwtConfig = mock[JwtConfig]
    override val apiKey: ApiKey       = ApiKey("test", "abcd")
    val cookieName: String            = "cookie-name"
  }

  val userInfo = UserInfo(
    "userId",
    "admin",
    "user",
    "admin@user.com",
    Role.GuildAdmin()
  )

  val jwtId                = "1234"
  val controllerComponents = mock[ControllerComponents]
  (controllerComponents.executionContext _).expects.atLeastOnce.returns(global)

  val formResourcesRepo = stub[FormResourceRepository]
  val fileUploadClient = new FileUploadClient(
    mock[AuthenticationClientConfig],
    mock[WSClient],
    controllerComponents
  ) {
    override def uploadEndpoint: (
        (Source[MultipartFormData.Part[Source[ByteString, Any]], Any], String)
    ) => Future[String] = _ => Future.successful("fileId")
  }
  val documentAnalyserClient = new DocumentAnalyserClient(
    mock[MicroServiceConfig],
    mock[WSClient],
    controllerComponents
  ) {
    override def analyseForm: Source[MultipartFormData.Part[
      Source[ByteString, Any]
    ], Any] => Future[Either[Invalid, Seq[FormResourceFieldDescriptor]]] =
      _ => Future.successful(Right(Seq()))
  }
  val formResourcesService = new FormResourcesService(
    formResourcesRepo,
    fileUploadClient,
    documentAnalyserClient
  )

  it("should succeed if the file is a docx file") {

    val formResourceDetails = FormResourceDetails(
      "name",
      "notes",
      List(FormCategory("Category")),
      true
    )

    val formResource = FormResource(
      "resourceId",
      formResourceDetails,
      PublicOwnership(),
      Seq(),
      None
    )

    val formData = MultipartFormData.apply(
      Map("details" -> Seq(formResourceDetails.asJson.noSpaces)),
      Seq(
        MultipartFormData.FilePart(
          "file",
          "someFile",
          Some(FormResourcesService.DOCX_MIME_TYPE),
          Files.SingletonTemporaryFileCreator.create()
        )
      ),
      Seq()
    )

    (
        (
            details: FormResourceDetails,
            info: UserInfo,
            seq: Seq[FormResourceFieldDescriptor],
            ec: ExecutionContext
        ) =>
          formResourcesRepo
            .insertResource(details, info, seq)(ec)
    )
      .when(*, *, *, *)
      .returns(Future.successful(formResource))

    val result = Await.result(
      formResourcesService.insertResource(formData, userInfo, jwtId),
      Duration.Inf
    )

    assertResult(formResource)(result)

  }
  it("should fail if the file is not a docx file") {

    val formResourceDetails = FormResourceDetails(
      "name",
      "notes",
      List(FormCategory("Category")),
      true
    )

    val formResource = FormResource(
      "resourceId",
      formResourceDetails,
      PublicOwnership(),
      Seq(),
      None
    )

    val formData = MultipartFormData.apply(
      Map("details" -> Seq(formResourceDetails.asJson.noSpaces)),
      Seq(
        MultipartFormData.FilePart(
          "file",
          "someFile",
          Some("text/plain"),
          Files.SingletonTemporaryFileCreator.create()
        )
      ),
      Seq()
    )

    val caught = intercept[Throwable](
      Await.result(
        formResourcesService.insertResource(formData, userInfo, jwtId),
        Duration.Inf
      )
    )

    assertResult("File must be a docx")(caught.getMessage())

  }

}
