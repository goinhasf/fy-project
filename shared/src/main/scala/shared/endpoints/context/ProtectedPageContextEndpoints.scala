package shared.endpoints.context

import endpoints4s.algebra
import shared.endpoints.SecurityExtensions
import shared.pages.PageContext
import shared.pages.content.ContextContent

trait ProtectedPageContextEndpoints
    extends algebra.Endpoints
    with algebra.circe.JsonEntitiesFromCodecs
    with SecurityExtensions {

  def getPageContext = endpoint(
    csrfPost(
      path / "api" / "context",
      jsonRequest[PageContext],
      emptyRequestHeaders
    ),
    sessionResponse(jsonResponse[ContextContent])
  )
}
