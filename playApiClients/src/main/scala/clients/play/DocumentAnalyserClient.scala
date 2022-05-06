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
import shared.endpoints.DocumentAnalyserEndpoints

@Singleton()
class DocumentAnalyserClient @Inject() (
    microServiceConfig: MicroServiceConfig,
    ws: WSClient,
    cc: ControllerComponents
) extends client.Endpoints(microServiceConfig.serviceUrl, ws)(
      cc.executionContext
    )
    with DocumentAnalyserEndpoints
    with client.JsonEntitiesFromCodecs {

  type File = Source[ByteString, Any]
  type MultipartForm =
    Source[MultipartFormData.Part[Source[ByteString, Any]], Any]

  override def multipartFormRequest: RequestEntity[MultipartForm] =
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
