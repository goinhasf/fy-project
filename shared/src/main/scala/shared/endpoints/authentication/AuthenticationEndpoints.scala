package shared.endpoints.authentication
import endpoints4s.algebra
import shared.endpoints.authentication.Authentication
import shared.auth.{AuthRequestMethod, UserCredentialsAuth}

trait AuthenticationEndpoints
    extends algebra.Endpoints
    with algebra.circe.JsonEntitiesFromCodecs
    with Authentication {

  def loginResponse[A](implicit request: Request[A]) = unauthorizedResponse
    .orElse(authenticationToken)
    .xmap(_.toOption)(_.toRight())

  /** Login endpoint: takes the API key in a query string parameter and returns either `Some(authenticationToken)`
    * if the credentials are valid, or `None` otherwise
    */
  def login = {
    implicit val request = post(
      path / "api" / "login",
      jsonRequest[UserCredentialsAuth],
      headers = requestHeader("Csrf-Token")
    )
    endpoint(
      request,
      loginResponse
    )
  }

  /** Logout Endpoint: logs user out by revoking access token sent in Authentication header.
    *
    * @return
    */
  val logout: Endpoint[AuthenticationToken, Unit] = authenticatedEndpoint(
    Post,
    path / "api" / "logout",
    emptyRequest,
    emptyRequestHeaders,
    ok(emptyResponse)
  )
}
