package services.endpoints

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.SystemMaterializer
import akka.stream.scaladsl.FileIO
import akka.stream.scaladsl.Source
import akka.util.ByteString
import clients.play.DocumentAnalyserClient
import clients.play.FileUploadClient
import clients.play.auth.AuthorizationClient
import com.google.inject._
import dao.forms.FormResource
import dao.forms.FormResourceDetails
import dao.forms.FormResourceFieldDescriptor
import dao.forms.FormResourceFields
import dao.users.UserInfo
import data.repositories.FormResourceFieldsRepository
import data.repositories.FormResourceRepository
import endpoints4s.Invalid
import endpoints4s.play.server
import io.FileMetadata
import io.circe.Json
import io.circe.JsonObject
import io.circe._
import io.circe.parser._
import io.circe.syntax._
import org.bson.types.ObjectId
import play.api.Logger
import play.api.http.DefaultHttpErrorHandler
import play.api.http.HttpEntity
import play.api.libs.Files
import play.api.mvc.DefaultControllerComponents
import play.api.mvc.MultipartFormData
import play.api.mvc.Results
import play.api.mvc.Session
import services.EndpointService
import services.PlayComponentsProvider
import services.endpoints.handlers.ActionHandlers
import services.endpoints.handlers.JwtValidator
import services.endpoints.handlers.SessionHandler
import shared.config.MicroServiceConfig
import shared.endpoints.FormResourceEndpoints
import shared.server.endpoints.MultipartFormEndpoints

import scala.concurrent.Future
import scala.util.Failure
import scala.util.Success
import scala.util.Try

class FormResourceEndpointService @Inject() (
    formResourcesRepository: FormResourceRepository,
    controllerComponents: DefaultControllerComponents,
    fileUploadClient: FileUploadClient,
    documentAnalyserClient: DocumentAnalyserClient,
    override val authClient: AuthorizationClient,
    microServiceConfig: MicroServiceConfig,
    pc: PlayComponentsProvider,
    actorSystem: ActorSystem
) extends ServerAuthentication(
      microServiceConfig,
      authClient,
      pc
    )
    with FormResourceEndpoints
    with server.JsonEntitiesFromCodecs
    with ActionHandlers
    with SessionHandler
    with JwtValidator
    with MultipartFormEndpoints {

  implicit val materializer = SystemMaterializer.get(actorSystem).materializer
  val logger                = Logger(classOf[FormResourceEndpointService])

  val routes = routesFromEndpoints(
    insertResourceRoute,
    insertResourceWithExistingFileEndpoint,
    getResourceRoute,
    getAllForms,
    getFormResourceAsPdfRoute,
    updateFormResourceEndpoint,
    updateDefaultFieldValuesEndpoint,
    updateFormResourceEndpoint
  )

  def insertResourceWithExistingFileEndpoint = {
    def insertResourceHelper(
        details: FormResourceDetails,
        userInfo: UserInfo,
        jwtId: String
    ) =
      for {
        fileMetadata <- fileUploadClient
          .getFileMetadata(details.fileId.get, jwtId)
          .map(_ match {
            case Some(value) => value
            case None        => throw new Exception("FileMetadata not found")
          })
        file <- fileUploadClient
          .downloadFileEndpoint(
            details.fileId.get
          )
          .map(_ match {
            case Some(value) => value
            case None        => throw new Exception("File not found")
          })
        fieldMap <- documentAnalyserClient
          .analyseForm(
            Source.single(
              MultipartFormData.FilePart(
                "file",
                fileMetadata.fileName,
                fileMetadata.contentType,
                file,
                fileMetadata.size
              )
            )
          )
          .flatMap(
            _.fold(
              f => {
                logger.error(f.errors.reduceRight(_ + _))
                Future.failed(new Throwable(f.errors.reduceRight(_ + _)))
              },
              Future.successful(_)
            )
          )

        resource <- formResourcesRepository.insertResource(
          details,
          userInfo,
          fieldMap
        )
      } yield (resource)

    EndpointWithHandler2(
      insertResourceWithExistingFile,
      sessionOrAuthorizationValidator(Results.Unauthorized("jwt missing")),
      jwtValidatorFromHeader,
      (
          args: (FormResourceDetails, String, String),
          userInfo: UserInfo,
          jwtId: String
      ) => {
        val (data, _, _) = args
        insertResourceHelper(data, userInfo, jwtId)
      }
    )

  }

  def insertResourceRoute = {

    val docxMime =
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

    def insertResourceHelper(
        form: FormData,
        userInfo: UserInfo,
        jwtId: String
    ) = for {
      jsonFormResourceString <- form
        .dataParts
        .get("details")
        .map(_.fold("")(_ + _))
        .toRight(new Throwable("Could not find 'details' in form data"))
        .fold(Future.failed(_), Future.successful(_))
      formDetails <- decode[FormResourceDetails](jsonFormResourceString)
        .fold(Future.failed(_), Future.successful(_))
      file <- form
        .files
        .headOption
        .toRight(new Throwable("No file found in form"))
        .flatMap { file =>
          if (file.contentType.map(_ == docxMime).getOrElse(false)) {
            Right(file)
          } else {
            Left(new Throwable("File must be a docx"))
          }
        }
        .fold(Future.failed(_), Future.successful(_))
      fieldMap <- documentAnalyserClient
        .analyseForm(
          Source.single(convertToFileSource(file))
        )
        .flatMap(
          _.fold(
            f => Future.failed(new Throwable(f.errors.reduceRight(_ + _))),
            Future.successful(_)
          )
        )
      fileId <- fileUploadClient.uploadEndpoint(
        Source.single(convertToFileSource(file)),
        jwtId
      )
      resource <- formResourcesRepository
        .insertResource(
          FormResourceDetails(
            formDetails.name,
            formDetails.notes,
            formDetails.categories,
            formDetails.isPublic,
            Some(fileId)
          ),
          userInfo,
          fieldMap
        )
    } yield (resource)

    EndpointWithHandler2(
      insertResource,
      sessionOrAuthorizationValidator(Results.Unauthorized("jwt missing")),
      extractValueFromSession("jwtId"),
      (args: Tuple2[FormData, String], userInfo: UserInfo, jwtId: String) =>
        insertResourceHelper(args._1, userInfo, jwtId).transformWith(
          _ match {
            case Failure(exception) => {
              logger.error(exception.getMessage())
              exception.printStackTrace()
              Future.successful(Left(Invalid(exception.getMessage())))
            }
            case Success(value) => Future.successful(Right(value))
          }
        )
    )
  }

  def getResourceRoute = getResource
    .implementedByAsync { formResourcesRepository.findResource }

  def getAllForms = EndpointWithHandler1(
    getAllFormResources,
    sessionOrAuthorizationValidator(Results.Unauthorized("Jwt Missing")),
    (_: Unit, userInfo: UserInfo) =>
      formResourcesRepository
        .findAllResources(userInfo.id)
  )

  private def createFormParts(
      file: Source[ByteString, Any],
      metadata: FileMetadata,
      answers: Json
  ) = {
    val textData =
      MultipartFormData.DataPart(
        "data",
        answers.noSpaces
      )
    val fileData = MultipartFormData.FilePart(
      "file",
      metadata.fileName,
      metadata.contentType,
      file,
      metadata.size
    )
    Source(
      textData :: fileData :: List[
        MultipartFormData.Part[Source[ByteString, Any]]
      ]()
    )

  }

  private def obtainPdfSource(
      maybeMetadata: Option[FileMetadata],
      maybeFile: Option[Source[ByteString, Any]],
      answers: Json
  ) = {
    (for {
      file     <- maybeFile
      metadata <- maybeMetadata
    } yield (documentAnalyserClient.fillFormPdf(
      (true, createFormParts(file, metadata, answers))
    ))).fold(
      Future.failed[Source[ByteString, Any]](
        new Throwable("Could not find file info")
      )
    )(identity)
  }

  def getFormResourceAsPdfRoute = EndpointWithHandler2(
    formResourceAsPdf,
    sessionOrAuthorizationValidator(Results.Unauthorized("Jwt missing")),
    extractValueFromSession("jwtId"),
    (args: (String, Option[Json]), userInfo: UserInfo, jwtId: String) => {

      val (resourceId, maybeJsonAnswers) = args

      logger.debug(maybeJsonAnswers.get.toString())

      formResourcesRepository.findResource(resourceId).flatMap {
        _ match {
          case Some(value) => {
            for {
              maybeFile <- fileUploadClient
                .downloadFileEndpoint(value.details.fileId.get)
              maybeMetadata <- fileUploadClient
                .getFileMetadata(value.details.fileId.get, jwtId)
              pdfSource <- obtainPdfSource(
                maybeMetadata,
                maybeFile,
                maybeJsonAnswers
                  .orElse(value.defaultFieldValues.map(_.asJson))
                  .getOrElse(JsonObject.empty.asJson)
              ).flatMap(_.runReduce(_ ++ _))
            } yield (Some((pdfSource, Some("application/pdf"))))
          }
          case None => {
            Future.successful(None)
          }
        }
      }

    }
  )

  def updateFormResourceEndpoint = updateFormResource.implementedByAsync {
    args => formResourcesRepository.updateFormResourceDetails(args._1, args._2)
  }

  def updateDefaultFieldValuesEndpoint = EndpointWithHandler1(
    updateDefaultFieldValues,
    sessionOrAuthorizationValidator(Results.Unauthorized("Jwt Missing")),
    (args: (String, JsonObject, String), userInfo: UserInfo) => {
      formResourcesRepository.updateFormResourceDefaultFieldValues(
        args._1,
        args._2
      )
    }
  )

  private def convertToFileSource(
      file: MultipartFormData.FilePart[Files.TemporaryFile]
  ) =
    MultipartFormData.FilePart(
      file.key,
      file.filename,
      file.contentType,
      FileIO.fromPath(file.ref.toPath()),
      file.fileSize,
      file.dispositionType
    )

}
