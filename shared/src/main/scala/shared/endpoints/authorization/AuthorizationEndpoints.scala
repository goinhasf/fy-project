package shared.endpoints.authorization

import endpoints4s.algebra
import shared.endpoints.authentication.Authentication
import shared.auth.UserCredentialsAuth
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

trait AuthorizationEndpoints
    extends algebra.Endpoints
    with algebra.circe.JsonEntitiesFromCodecs {

  val createToken = endpoint(
    post(path / "auth" / "token", jsonRequest[UserCredentialsAuth]),
    ok(textResponse)
  )

  val authorize = endpoint(
    get(
      path / "auth" /? qs[String]("token")
    ),
    response(Unauthorized, emptyResponse)
      .orElse(ok(textResponse))
      .xmap {
        _ match {
          case Left(value)  => None
          case Right(value) => Some(value)
        }
      }(optString => optString.toRight(()))
  )

  val getTokenContent = endpoint(
    get(
      path / "auth" / "content" /? qs[String]("token")
    ),
    response(Unauthorized, emptyResponse)
      .orElse(ok(textResponse))
      .xmap {
        _ match {
          case Left(value)  => None
          case Right(value) => Some(value)
        }
      }(optString => optString.toRight(()))
  )

}
