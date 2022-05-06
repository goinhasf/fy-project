package services.endpoints

import play.api.http.Writeable
import endpoints4s.play.server
import play.api.mvc.BodyParser
import play.api.mvc.Action
import play.api.http.HttpEntity
import play.api.mvc.Results
import views.AppTemplate
import play.api.http.ContentTypes

trait HtmlEndpoints
    extends shared.endpoints.HtmlEndpoints
    with server.Endpoints {
  type HtmlPage = AppTemplate
  def pageResponse: ResponseEntity[HtmlPage] = page =>
    AppTemplate
      .writeableView
      .toEntity(page)
      .as(ContentTypes.HTML)
}
