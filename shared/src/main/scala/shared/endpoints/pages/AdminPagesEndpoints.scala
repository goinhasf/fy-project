package shared.endpoints.pages

import shared.endpoints.SecurityExtensions
import shared.endpoints.HtmlEndpoints

trait AdminPagesEndpoints extends HtmlEndpoints with SecurityExtensions {

  def adminLoginPage = endpoint(
    get(path / "admin" / "login"),
    ok(pageResponse)
  )

  def createProtectedPageEndpoint[A](
      requestPath: Path[A]
  ) = endpoint(
    get(requestPath),
    sessionResponse(pageResponse)
  )

  def adminMainPage = createProtectedPageEndpoint(path / "admin")
  def adminEventSubmissionsPage = createProtectedPageEndpoint(
    path / "admin" / "event-submissions"
  )

}
