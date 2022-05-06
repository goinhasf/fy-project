package shared.server.endpoints

import endpoints4s.play.server
import akka.util.ByteString
import play.api.mvc.MultipartFormData
import play.api.libs.Files
import play.api.http.HttpEntity

trait MultipartFormEndpoints
    extends shared.endpoints.MultipartFormEndpoints
    with server.Endpoints {

  type File     = (ByteString, Option[String])
  type FormData = MultipartFormData[Files.TemporaryFile]
  val payloadBodyParser = playComponents.playBodyParsers.multipartFormData

  protected override def fileResponseEntity: File => HttpEntity = file => {
    HttpEntity.Strict.tupled(file)
  }

  protected override def multipartFormRequestEntity: RequestEntity[FormData] =
    req => Some(payloadBodyParser)

}
