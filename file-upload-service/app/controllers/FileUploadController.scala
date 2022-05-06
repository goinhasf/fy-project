package controllers

import java.io.{File => JFile}
import java.nio.file.Paths
import java.util.UUID

import scala.concurrent.Future

import akka.actor.ActorSystem
import akka.stream.IOResult
import akka.stream.Materializer
import akka.stream.scaladsl.FileIO
import akka.util.ByteString
import com.google.inject._
import com.typesafe.config.Config
import shared.endpoints.FileUploadEndpoints
import endpoints4s.play.server
import io.FileMetadata
import play.api.Logger
import play.api.http.DefaultHttpErrorHandler
import play.api.http.HttpChunk
import play.api.http.HttpEntity
import play.api.libs.Files.TemporaryFile
import play.api.libs.Files.TemporaryFileCreator
import play.api.libs.streams.Accumulator
import play.api.mvc.ControllerComponents
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData.FilePart
import play.api.routing.Router
import play.api.routing.SimpleRouter
import repositories.FileMetadataRepository

import controllers.PlayComponentsController
import shared.server.endpoints.MultipartFormEndpoints
import akka.stream.SystemMaterializer
import clients.play.auth.AuthorizationClient
import shared.config.MicroServiceConfig
import utils.FileUploadConfig

@Singleton
class FileUploadController @Inject() (
    actorSystem: ActorSystem,
    override val cc: ControllerComponents,
    errorHandler: DefaultHttpErrorHandler,
    config: FileUploadConfig,
    fileMetadataRepository: FileMetadataRepository,
    temporaryFileCreator: TemporaryFileCreator,
    authService: AuthorizationClient
) extends ServerAuthentication(config, errorHandler, cc, authService)
    with server.Endpoints
    with server.JsonEntitiesFromCodecs
    with FileUploadEndpoints
    with SimpleRouter
    with MultipartFormEndpoints {

  implicit val materializer = SystemMaterializer.get(actorSystem).materializer

  val logger = Logger(classOf[FileUploadController])

  override def routes: Router.Routes = routesFromEndpoints(
    upload,
    getFileMetadataEndpoint,
    downloadFile,
    updateFile
  )

  def upload = uploadEndpoint.implementedByAsync { args =>
    val (formData, jwtId) = args

    authService
      .authorize(jwtId.id)
      .collect({ case Some(value) => value })
      .recover({ case t: NoSuchElementException =>
        logger.info("Authorization failed")
        throw new IllegalAccessException("Authorization failed")
      })
      .flatMap { _ =>
        formData.files.headOption match {
          case Some(file) => {
            val id = UUID.randomUUID().toString()
            saveFile(id, file).flatMap { _ =>
              fileMetadataRepository
                .saveMetadata(
                  FileMetadata(
                    id,
                    file.filename,
                    file.fileSize,
                    file.contentType
                  )
                )
                .map(_ => id)

            }
          }
          case None => {
            logger.info("No file in multipart form")
            Future.failed(new Throwable("No file in multipart form found"))
          }
        }
      }

  }

  def updateFile = updateFileEndpoint.implementedByAsync { serviceArgs =>
    serviceArgs._2.files.headOption match {
      case Some(file) =>
        fileMetadataRepository
          .updateMetadata(
            serviceArgs._1,
            FileMetadata(
              serviceArgs._1,
              file.filename,
              file.fileSize,
              file.contentType
            )
          )
          .flatMap {
            _ match {
              case Some(id) => saveFile(id, file).map(_ => Some(id))
              case None     => Future.failed(new Throwable("File not found"))
            }

          }
      case None => Future.failed(new Throwable("No file found in form data"))
    }
  }

  def getFileMetadataEndpoint = getFileMetadata.implementedByAsync { args =>
    val (id, jwtId) = args
    authService
      .authorize(jwtId.id)
      .collect({ case Some(value) => value })
      .recover({ case t: NoSuchElementException =>
        logger.info("Authorization failed")
        throw new IllegalAccessException("Authorization failed")
      })
      .flatMap { _ =>
        fileMetadataRepository.getFileMetadata(id)
      }
  }

  def downloadFile = downloadFileEndpoint.implementedByAsync { fileId =>
    fileMetadataRepository.getFileMetadata(fileId).flatMap { optFileMetadata =>
      optFileMetadata.map { metadata =>
        val path = new JFile(s"${config.filePath}/${metadata.fileId}").toPath()

        if (!path.toFile().exists()) {
          Future.failed(new Throwable("File not found"))
        } else {
          val byteString =
            FileIO.fromPath(path).runReduce(_ ++ _)
          byteString.map(s => Some((s, metadata.contentType)))
        }
      } getOrElse (Future.failed(new Throwable("File not found")))
    }
  }

  private def saveFile(
      fileId: String,
      file: FilePart[TemporaryFile]
  ): Future[IOResult] = {
    val path = Paths.get(config.filePath, fileId)
    logger.debug(s"Allowed to write ${file.ref.canWrite()}")
    val fileSink    = FileIO.toPath(path).async
    val accumulator = Accumulator(fileSink)
    accumulator.run(FileIO.fromPath(file.ref.path))
  }

}
