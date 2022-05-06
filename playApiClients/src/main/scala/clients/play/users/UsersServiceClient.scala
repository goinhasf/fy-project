package clients.play.users
import com.google.inject._
import shared.config.MicroServiceConfig
import play.api.libs.ws.WSClient
import play.api.mvc.ControllerComponents
import endpoints4s.play.client
import shared.endpoints.user.UserPrivilegeEndpoints
import dao.users.UserInfo
import endpoints4s.Invalid
import scala.concurrent.Future
import shared.endpoints.user.AddPrivilegesPayload
import shared.auth.Role
import shared.auth.Privilege
import shared.endpoints.UserInfoEndpoints

class UsersServiceClient @Inject() (
    microServiceConfig: MicroServiceConfig,
    ws: WSClient,
    cc: ControllerComponents
) extends client.Endpoints(microServiceConfig.serviceUrl, ws)(
      cc.executionContext
    )
    with UserPrivilegeEndpoints
    with client.JsonEntitiesFromCodecs {

  def addUserPrivilege(
      userId: String,
      newPrivileges: Set[Privilege]
  ): Future[Either[Invalid, Option[UserInfo]]] = super.addPrivileges(
    (userId, AddPrivilegesPayload(microServiceConfig.apiKey, newPrivileges))
  )
}
