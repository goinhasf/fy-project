package shared.endpoints

import endpoints4s.algebra
import dao.societies.Society

trait SocietiesEndpoints
    extends algebra.Endpoints
    with algebra.circe.JsonEntitiesFromCodecs
    with SecurityExtensions
    with MultipartFormEndpoints {

  def restPath = path / "api" / "register-your-society"

  def createSociety = endpoint(
    csrfPost(
      restPath,
      multipartFormRequestEntity,
      emptyRequestHeaders
    ),
    badRequest().orElse(ok(jsonResponse[Society]))
  )

  def getSociety = endpoint(
    get(restPath),
    ok(jsonResponse[Society]).orNotFound()
  )

  def updateSociety = endpoint(
    csrfPut(
      restPath,
      multipartFormRequestEntity,
      emptyRequestHeaders
    ),
    badRequest().orElse(ok(jsonResponse[Society]))
  )

}
