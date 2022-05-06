package services

import endpoints4s.xhr
import services.util.DefaultCsrfTokenConsumer
import scala.util.Try
import endpoints4s.Tupler
import shared.endpoints.authentication.Authentication

trait ClientAuthentication
    extends XHRObservableEndpoints
    with xhr.JsonEntitiesFromCodecs
    with Authentication {
  // The constructor is private so that users can not
  // forge instances themselves
  type AuthenticationToken = String

  // Decodes the user info from an OK response
  def authenticationToken[A](implicit
      request: Request[A]
  ): Response[AuthenticationToken] = { request =>
    if (request.status == OK) {
      Try(request.getResponseHeader("Authorization")).toOption match {
        case Some(headerValue) =>
          Some(_ => Right(headerValue.stripPrefix("Bearer ")))
        case _ => Some(_ => Left(new Exception("Missing JWT session")))
      }
    } else None
  }

  protected def authenticatedRequest[U, E, H, UE, HCred, Out](
      method: Method,
      url: Url[U],
      entity: RequestEntity[E],
      headers: RequestHeaders[H],
      requestDocs: Option[String]
  )(implicit
      tuplerUE: Tupler.Aux[U, E, UE],
      tuplerHCred: Tupler.Aux[
        H,
        AuthenticationToken,
        HCred
      ],
      tuplerUEHCred: Tupler.Aux[UE, HCred, Out]
  ): Request[Out] = {
    // Encodes the user info as a JWT object in the `Authorization` request header
    val authenticationTokenRequestHeaders
        : RequestHeaders[AuthenticationToken] = { (token, request) =>
      request.setRequestHeader(
        "Authorization",
        s"Bearer ${token}"
      )
    }
    val allHeaders = headers ++ authenticationTokenRequestHeaders
    request(method, url, entity, requestDocs, allHeaders)

  }
// Checks that the response is not `Unauthorized` before continuing
  def wheneverAuthenticated[A, B](
      response: Response[B]
  )(implicit request: Request[A]): Response[B] = { request =>
    if (request.status == Unauthorized) {
      Some(_ => Left(new Exception("Unauthorized")))
    } else {
      response(request)
    }
  }
}
