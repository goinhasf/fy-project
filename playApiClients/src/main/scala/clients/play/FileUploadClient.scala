package clients.play

import shared.endpoints.FileUploadEndpoints
import endpoints4s.play.client
import com.google.inject._
import play.api.mvc.ControllerComponents
import play.api.libs.ws.WSClient
import play.api.libs.ws.WSResponse
import akka.stream.scaladsl.Source
import akka.util.ByteString
import play.api.mvc.MultipartFormData
import play.api.Logger
import shared.config.MicroServiceConfig
import scala.concurrent.Future
import akka.stream.IOResult
import clients.play.auth.config.AuthenticationClientConfig
import auth.AuthTrait

@Singleton()
class FileUploadClient @Inject() (
    microServiceConfig: AuthenticationClientConfig,
    ws: WSClient,
    cc: ControllerComponents
) extends AuthTrait(microServiceConfig, ws, cc)
    with FileUploadEndpoints
    with client.JsonEntitiesFromCodecs {

  type File = Source[ByteString, Any]
  type FormData =
    Source[MultipartFormData.Part[Source[ByteString, Any]], Any]

  override def multipartFormRequestEntity: RequestEntity[FormData] =
    (formData, request) => {
      request.withBody(formData)
    }

  override def fileResponseEntity: WSResponse => Either[Throwable, File] =
    response => {
      if (response.status == OK) {
        Right(response.bodyAsSource)
      } else {
        Left(new Throwable(s"File response: ${response.status}"))
      }
    }
}
