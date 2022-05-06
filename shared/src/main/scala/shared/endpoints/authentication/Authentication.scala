package shared.endpoints.authentication

import endpoints4s.algebra
import endpoints4s.Tupler
import shared.auth.UserCredentialsAuth

/** Algebra interface for defining authenticated endpoints using JWT.
  */
trait Authentication extends algebra.Endpoints with algebra.circe.JsonEntitiesFromCodecs {

  /** Authentication information */
  type AuthenticationToken

  /** A response entity containing the authenticated user info
    *
    * Clients decode the JWT attached to the response.
    * Servers encode the authentication information as a JWT and attach it to their response.
    */
  def authenticationToken[A](implicit
      request: Request[A]
  ): Response[AuthenticationToken]

  def unauthorizedResponse = response(Unauthorized, emptyResponse)

  def wheneverCredentialsValid[A](implicit
      request: Request[A]
  ): Response[Option[AuthenticationToken]] =
    authenticationToken
      .orElse(unauthorizedResponse)
      .xmap {
        _ match {
          case Left(value)  => Some(value)
          case Right(value) => None
        }
      }(optString => optString.toLeft(()))

  /** A response that might signal to the client that his request was invalid using
    * a `BadRequest` status.
    * Clients map `BadRequest` statuses to `None`, and the underlying `response` into `Some`.
    * Conversely, servers build a `BadRequest` response on `None`, or the underlying `response` otherwise.
    */
  final def wheneverValid[A, B](responseA: Response[B])(implicit
      request: Request[A]
  ): Response[Option[B]] =
    responseA
      .orElse(response(BadRequest, emptyResponse))
      .xmap(_.fold[Option[B]](Some(_), _ => None))(_.toLeft(()))

  /** A request with the given `method`, `url` and `entity`, and which is rejected by the server if it
    * doesn’t contain a valid JWT.
    */
  protected def authenticatedRequest[U, E, H, UE, HCred, Out](
      method: Method,
      url: Url[U],
      entity: RequestEntity[E],
      headers: RequestHeaders[H],
      requestDocs: Option[String]
  )(implicit
      tuplerUE: Tupler.Aux[U, E, UE],
      tuplerHCred: Tupler.Aux[H, AuthenticationToken, HCred],
      tuplerUEHCred: Tupler.Aux[UE, HCred, Out]
  ): Request[Out]

  /** A response that might signal to the client that his request was not authenticated.
    * Clients throw an exception if the response status is `Unauthorized`.
    * Servers build an `Unauthorized` response in case the incoming request was not correctly authenticated.
    */
  protected def wheneverAuthenticated[A, B](
      response: Response[B]
  )(implicit request: Request[A]): Response[B]

  /** User-facing constructor for endpoints requiring authentication.
    *
    * @return An endpoint requiring a authentication information to be provided
    *         in the `Authorization` request header. It returns `response`
    *         if the request is correctly authenticated, otherwise it returns
    *         an empty `Unauthorized` response.
    *
    * @param method        HTTP method
    * @param url           Request URL
    * @param response      HTTP response
    * @param requestEntity HTTP request entity
    * @tparam U Information carried by the URL
    * @tparam E Information carried by the request entity
    * @tparam R Information carried by the response
    */
  def authenticatedEndpoint[U, E, H, UE, HCred, UEHCred, R](
      method: Method,
      url: Url[U],
      entity: RequestEntity[E],
      headers: RequestHeaders[H],
      response: Response[R],
      requestDocs: Option[String] = None
  )(implicit
      tuplerUE: Tupler.Aux[U, E, UE],
      tuplerHCred: Tupler.Aux[H, AuthenticationToken, HCred],
      tuplerUEHCred: Tupler.Aux[UE, HCred, UEHCred]
  ): Endpoint[UEHCred, R] = {
    implicit val request =
      authenticatedRequest(method, url, entity, headers, requestDocs)
    endpoint(
      request,
      wheneverAuthenticated(response)
    )
  }

}
