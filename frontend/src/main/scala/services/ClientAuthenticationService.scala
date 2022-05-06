package services

import endpoints4s.xhr
import shared.endpoints.authentication.Authentication
import dao.users.UserInfo
import scala.util.Try
import java.nio.file.attribute.UserPrincipal
import endpoints4s.Tupler
import shared.endpoints.authentication.AuthenticationEndpoints
import endpoints4s.Invalid
import util.DefaultCsrfTokenConsumer
import shared.auth.UserCredentialsAuth
import com.raquo.airstream.core.EventStream

/** Interpreter for the [[Authentication]] algebra interface that produces
  * a Play client (using `play.api.libs.ws.WSClient`).
  */
object ClientAuthenticationService
    extends ClientService
    with ClientAuthentication
    with AuthenticationEndpoints
    with xhr.JsonEntitiesFromCodecs
    with DefaultCsrfTokenConsumer {

  def login(cred: UserCredentialsAuth): EventStream[Option[String]] =
    super.login((cred, getCsrfToken.get))

}
