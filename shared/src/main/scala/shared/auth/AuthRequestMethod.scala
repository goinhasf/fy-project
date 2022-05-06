package shared.auth
import io.circe.generic.semiauto._
import io.circe.generic.JsonCodec

sealed trait AuthRequestMethod
case class ApiKeyAuth(apiKey: String) extends AuthRequestMethod

@JsonCodec
case class UserCredentialsAuth(username: String, password: String)
    extends AuthRequestMethod
