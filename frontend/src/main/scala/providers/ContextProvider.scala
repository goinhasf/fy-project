package providers

import scala.concurrent.Future

import com.raquo.airstream.core.EventStream
import endpoints4s.Invalid
import endpoints4s.Valid
import endpoints4s.Validated
import endpoints4s.algebra.MuxRequest
import endpoints4s.xhr
import io.circe.Json
import services.XHRObservableMuxEndpoints
import shared.endpoints.context.PageContextEndpoints
import shared.pages.PageContext
import shared.pages.content.ContextContent
import endpoints4s.algebra.circe.CirceCodec
import services.util.DefaultCsrfTokenConsumer
import services.ClientService

trait ContextProvider[A <: ContextContent]
    extends xhr.MuxEndpoints
    with xhr.JsonEntitiesFromCodecs
    with XHRObservableMuxEndpoints
    with PageContextEndpoints
    with ClientService
    with DefaultCsrfTokenConsumer {
  def getContext(): EventStream[A]
}
