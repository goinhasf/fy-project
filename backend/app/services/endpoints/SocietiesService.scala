package services.endpoints

import com.google.inject._
import clients.play.FileUploadClient
import services.PlayComponentsProvider
import akka.actor.ActorSystem
import services.EndpointService
import endpoints4s.play.server
import services.endpoints.handlers.ActionHandlers
import services.endpoints.handlers.SessionHandler
import shared.server.endpoints.MultipartFormEndpoints
import play.api.routing.Router
import services.endpoints.handlers.JwtValidator
import dao.societies.Society
import dao.users.UserInfo
import data.repositories.SocietiesRepository
import io.circe.parser._
import dao.societies.SocietyDetails
import endpoints4s.Invalid
import clients.play.auth.AuthorizationClient
import play.api.mvc.Results
import clients.play.users.UsersServiceClient
import org.bson.types.ObjectId
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.FileIO
import play.api.mvc.MultipartFormData
import play.api.libs.Files
import scala.concurrent.Future
import shared.config.MicroServiceConfig
import shared.auth.Role
import shared.auth.Privilege
import shared.endpoints.user.AddPrivilegesPayload

@Singleton
class SocietiesService @Inject() (
    societiesRepository: SocietiesRepository,
    usersServiceClient: UsersServiceClient,
    fileUploadClient: FileUploadClient,
    pc: PlayComponentsProvider,
    actorSystem: ActorSystem,
    override val authClient: AuthorizationClient,
    config: MicroServiceConfig
) extends EndpointService(
      pc
    )
    with shared.endpoints.SocietiesEndpoints
    with server.JsonEntitiesFromCodecs
    with ActionHandlers
    with SessionHandler
    with JwtValidator
    with MultipartFormEndpoints {

  implicit val jwtConfig = config.jwtConfig

  def routes: Router.Routes = routesFromEndpoints(createSocietyEndpoint)

  val createSocietyEndpoint = EndpointWithHandler2(
    createSociety,
    sessionOrAuthorizationValidator(
      Results.Unauthorized("Jwt not found in session or header")
    ),
    extractValueFromSession("jwtId"),
    createSocietyService
  )


  def createSocietyService(
      args: (FormData, String),
      userInfo: UserInfo,
      jwtId: String
  ): Future[Either[Invalid, Society]] = {

    def createSocietyHelper(
        societyDetails: SocietyDetails,
        maybePicture: Option[MultipartFormData.FilePart[Files.TemporaryFile]]
    ) = {
      societiesRepository.getSocietyWithName(societyDetails.name).flatMap {
        _ match {
          case Some(society) => Future.successful(None)
          case None => {
            val id = new ObjectId()
            val createSociety = maybePicture.map(file =>
              fileUploadClient
                .uploadEndpoint(
                  Source.single(tempFileToSource(file)),
                  jwtId
                )
            ) match {
              case Some(value) =>
                value.map(fileId =>
                  Society(id.toHexString(), societyDetails, Some(fileId))
                )
              case None =>
                Future
                  .successful(Society(id.toHexString(), societyDetails, None))
            }
            createSociety
              .flatMap(societiesRepository.insertSociety)
              .map(Some(_))
          }
        }
      }

    }

    def addSocietyHelper(society: Society) = usersServiceClient
      .addPrivileges(
        (
          userInfo.id,
          AddPrivilegesPayload(
            config.apiKey,
            Privilege.committeeAdminPrivileges(society._id)
          )
        )
      )
      .map(maybeUpdate =>
        maybeUpdate match {
          case Left(value) => Left(value)
          case Right(value) =>
            value
              .map(_ => society)
              .toRight(Invalid("User info privilege update failed"))
        }
      )

    val (form, _) = args
    def parseFormData = for {
      details <- form
        .dataParts
        .get("details")
        .map(_.reduceRight(_ + _))
        .toRight(
          Invalid(
            "Could not find 'details' field in multipart-form data"
          )
        )
      parsedDetails <- decode[SocietyDetails](details)
        .left
        .map(error => Invalid(error.getMessage()))
      maybeFile = form.file("file")
    } yield createSocietyHelper(parsedDetails, maybeFile)
      .flatMap(_ match {
        case Some(value) => addSocietyHelper(value)
        case None =>
          Future.successful(
            Left(Invalid("Society with this name already exists"))
          )
      })

    parseFormData.fold(
      invalid => Future.successful(Left(invalid)),
      fSociety => fSociety
    )
  }

  private def tempFileToSource(
      file: MultipartFormData.FilePart[Files.TemporaryFile]
  ) =
    MultipartFormData.FilePart(
      "file",
      file.filename,
      file.contentType,
      FileIO.fromPath(file.ref.path),
      file.fileSize,
      file.dispositionType
    )

}
