package shared.endpoints
import endpoints4s.algebra
import dao.users.UserInfo
import shared.endpoints.authentication.Authentication

trait UserInfoEndpoints
    extends algebra.Endpoints
    with algebra.circe.JsonEntitiesFromCodecs
    with Authentication {

  def getUserInfo = authenticatedEndpoint(
    Get,
    path / "api" / "users" / "info" / segment[String]("id"),
    emptyRequest,
    emptyRequestHeaders,
    ok(jsonResponse[UserInfo]).orNotFound()
  )

  def getUserInfoApiKey = endpoint(
    get(
      path / "api" / "users" / "admin" / "info" / segment[String]("id"),
      headers = requestHeader("Service-Id") ++ requestHeader("Api-Key")
    ),
    ok(jsonResponse[UserInfo]).orNotFound()
  )
}
