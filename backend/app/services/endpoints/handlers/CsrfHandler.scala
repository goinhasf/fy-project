package services.endpoints.handlers

import play.filters.csrf.CSRF
import scala.concurrent.Future
import play.api.mvc.Results

trait CsrfHandler extends ActionHandlers {
  type CSRFExtractor = RequestHandler[CSRF.Token]
  def csrfExtractor: CSRFExtractor = request =>
    CSRF
      .getToken(request)
      .toRight(Results.BadRequest("Csrf token missing"))
}
