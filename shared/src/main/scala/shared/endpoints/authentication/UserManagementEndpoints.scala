package shared.endpoints.authentication
import endpoints4s.algebra
import shared.auth.RegisterUser

trait UserManagementEndpoints
    extends algebra.Endpoints
    with algebra.circe.JsonEntitiesFromCodecs {
  def register = endpoint(
    post(
      path / "api" / "user",
      jsonRequest[RegisterUser]
    ),
    ok(textResponse)
  )

  def remove = endpoint(
    delete(
      path / "api" / "user" / segment[String]("id") /? qs[String]("serviceId")
        .&(qs[String]("apiKey"))
    ),
    ok(emptyResponse).orNotFound()
  )

}
