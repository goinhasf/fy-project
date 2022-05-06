package services

import org.scalajs.dom.raw.XMLHttpRequest
import scala.scalajs.js
import org.scalajs.dom
import endpoints4s.xhr
import services.util.DefaultCsrfTokenConsumer
trait PageEndpointsDef
    extends ClientService
    with ClientAuthentication
    with DefaultCsrfTokenConsumer {
  type HtmlPage = String
  def pageResponse: ResponseEntity[HtmlPage] = req => {
    dom.document.location.href = req.responseURL.getOrElse("unknown")
    Right(req.responseText)
  }

}

object PageEndpoints
    extends PageEndpointsDef
    with shared.endpoints.pages.PageEndpoints {

  def submitSocietySelection(id: String) =
    super.submitSocietySelection((id, getCsrfToken().get))

}
