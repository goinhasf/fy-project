package dao.users

import shared.auth._
import io.circe.generic.JsonCodec

@JsonCodec
case class UserInfo(
    id: String,
    firstName: String,
    lastName: String,
    email: String,
    role: Role
)
object UserInfo {
  def createAdminInfo(
      id: String,
      firstName: String,
      lastName: String,
      email: String
  ) =
    UserInfo(
      id,
      firstName,
      lastName,
      email,
      Role.Admin()
    )
}
