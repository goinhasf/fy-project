package shared.endpoints
import endpoints4s.algebra
import endpoints4s.Tupler
import endpoints4s.Invalid

trait SecurityExtensions extends algebra.Endpoints {

  def csrfPost[U, E, H, HCsrf, UE, UEHCsrf](
      url: Url[U],
      requestEntity: RequestEntity[E],
      headers: RequestHeaders[H],
      docs: Option[String] = None
  )(implicit
      tuplerUE: Tupler.Aux[U, E, UE],
      tuplerHCsrf: Tupler.Aux[H, String, HCsrf],
      tuplerUEHCsrf: Tupler.Aux[UE, HCsrf, UEHCsrf]
  ) = post(url, requestEntity, docs, headers ++ requestHeader("Csrf-Token"))

  def csrfDelete[U, H, HCsrf, UHCsrf](
      url: Url[U],
      headers: RequestHeaders[H],
      docs: Option[String] = None
  )(implicit
      tuplerHCsrf: Tupler.Aux[H, String, HCsrf],
      tuplerUHCsrf: Tupler.Aux[U, HCsrf, UHCsrf]
  ) = delete(url, docs, headers ++ requestHeader("Csrf-Token"))

  def csrfPut[U, E, H, HCsrf, UE, UEHCsrf](
      url: Url[U],
      requestEntity: RequestEntity[E],
      headers: RequestHeaders[H],
      docs: Option[String] = None
  )(implicit
      tuplerUE: Tupler.Aux[U, E, UE],
      tuplerHCsrf: Tupler.Aux[H, String, HCsrf],
      tuplerUEHCsrf: Tupler.Aux[UE, HCsrf, UEHCsrf]
  ) = put(url, requestEntity, docs, headers ++ requestHeader("Csrf-Token"))

  def csrfNotFound: Response[ClientErrors] =
    response(BadRequest, clientErrorsResponseEntity, None)
  def csrfResponse[A](
      response: Response[A]
  ): Response[Either[ClientErrors, A]] = csrfNotFound.orElse(response)

  def sessionResponse[A](
      response: ResponseEntity[A]
  ): Response[Option[A]] = badRequest()
    .orElse(ok(response))
    .xmap(_.toOption)(_.toRight(Invalid("Session not found")))
}
