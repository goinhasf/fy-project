package shared.auth
import io.circe.generic.semiauto._
import io.circe.generic.extras.ConfiguredJsonCodec

@ConfiguredJsonCodec
sealed trait RoleType
case class AdminRole()           extends RoleType
case class GuildAdminRole()      extends RoleType
case class RegularUserRole()     extends RoleType
case class CommitteeMemberRole() extends RoleType
case class CommitteeAdminRole()  extends RoleType
case class TestRole()            extends RoleType

object RoleType {
  import io.circe.generic.extras.auto._
  import io.circe.generic.extras.Configuration
  implicit val genDevConfig: Configuration = Configuration
    .default
    .withDiscriminator("_t")
}

case class Role(
    roleType: RoleType,
    privileges: Set[Privilege] = Set()
)
object Role {
  implicit val codec = deriveCodec[Role]
  import Privilege._

  def Admin() = Role(
    AdminRole(),
    Set(
      SystemResourcesManagement(),
      SystemUsersManagement()
    )
  )
  def GuildAdmin() = Role(
    GuildAdminRole(),
    Set(SystemResourcesManagement())
  )
  def RegularUser(privileges: Set[Privilege]) = Role(
    RegularUserRole(),
    privileges
  )
  def TestUser(privileges: Set[Privilege] = Set()) = Role(
    TestRole(),
    privileges
  )
}
