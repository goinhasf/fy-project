package shared.endpoints.user

import shared.auth.ApiKey
import io.circe.generic.semiauto._
import shared.auth.Privilege

case class AddPrivilegesPayload(
    apiKey: ApiKey,
    newPrivileges: Set[Privilege]
)
object AddPrivilegesPayload {
  implicit val codec = deriveCodec[AddPrivilegesPayload]
}
