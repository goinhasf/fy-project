package shared.auth

import io.circe.generic.JsonCodec

@JsonCodec
case class Privilege(
    name: String,
    scopes: Map[String, Seq[String]] = Map()
)

object Privilege {

  val SOCIETIES_KEY = "societies"

  def CommitteeUsersManagement(committeeId: String) = Privilege(
    "committee-users-management",
    Map(SOCIETIES_KEY -> Seq(committeeId))
  )
  def SystemResourcesManagement() = Privilege(
    "system-resources-management"
  )
  def SystemUsersManagement() = Privilege(
    "system-users-management"
  )
  def CommitteeResourceManagement(committeeId: String) = Privilege(
    "committee-resource-management",
    Map(SOCIETIES_KEY -> Seq(committeeId))
  )

  def committeeAdminPrivileges(committeeId: String) = Set(
    CommitteeResourceManagement(committeeId),
    CommitteeUsersManagement(committeeId)
  )
}
