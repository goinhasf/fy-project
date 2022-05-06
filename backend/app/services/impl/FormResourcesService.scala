package services.impl

import com.google.inject._
import data.repositories.FormResourceRepository
import clients.play.FileUploadClient
import clients.play.DocumentAnalyserClient
import dao.forms.FormResourceDetails
import dao.users.UserInfo
import scala.concurrent.ExecutionContext
import akka.stream.scaladsl.Source
import play.api.mvc.MultipartFormData
import scala.concurrent.Future
import play.api.Logger
import dao.forms.FormResource
import play.api.libs.Files
import io.circe._
import io.circe.parser._
import io.circe.syntax._
import akka.stream.scaladsl.FileIO

@Singleton
class FormResourcesService @Inject() (
    formResourcesRepository: FormResourceRepository,
    fileUploadClient: FileUploadClient,
    documentAnalyserClient: DocumentAnalyserClient
) {

  type FormData = MultipartFormData[Files.TemporaryFile]

  val logger = Logger(classOf[FormResourcesService])

  def insertResource(
      form: FormData,
      userInfo: UserInfo,
      jwtId: String
  )(implicit ec: ExecutionContext): Future[FormResource] =
    for {
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
          if (file.contentType.map(_ == FormResourcesService.DOCX_MIME_TYPE).getOrElse(false)) {
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
object FormResourcesService {
  val DOCX_MIME_TYPE =
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
}
