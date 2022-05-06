package shared.endpoints.user

import endpoints4s.algebra
import dao.users.UserInfo
import endpoints4s.Invalid
import shared.auth.ApiKey
import shared.auth.Role

trait UserPrivilegeEndpoints
    extends algebra.Endpoints
    with algebra.circe.JsonEntitiesFromCodecs {

  val restPath = path / "api" / "user" / "role"

  private def unauthorizedResponse =
    response(Unauthorized, clientErrorsResponseEntity)

  /** Takes the user id as query param and an api key
    *
    * @return The updated user info if found, else Not Found or even Unauthorized.
    */
  def addPrivileges = endpoint(
    post(restPath /? qs[String]("userId"), jsonRequest[AddPrivilegesPayload]),
    unauthorizedResponse.orElse(
      ok(jsonResponse[UserInfo]).orNotFound()
    )
  )

}
