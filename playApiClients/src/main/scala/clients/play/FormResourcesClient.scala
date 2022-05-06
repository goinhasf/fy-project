package clients.play

import com.google.inject.Inject
import shared.config.MicroServiceConfig
import play.api.libs.ws.WSClient
import endpoints4s.play.client
import shared.endpoints.FormResourceEndpoints
import play.api.mvc.ControllerComponents
import akka.stream.scaladsl.Source
import play.api.mvc.MultipartFormData
import akka.util.ByteString
import play.api.libs.ws.WSResponse

class FormResourcesClient @Inject() (
    microServiceConfig: MicroServiceConfig,
    ws: WSClient,
    cc: ControllerComponents
) extends client.Endpoints(microServiceConfig.serviceUrl, ws)(
      cc.executionContext
    )
    with FormResourceEndpoints
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
