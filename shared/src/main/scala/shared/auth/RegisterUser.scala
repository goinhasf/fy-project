package shared.auth

import dao.users.UserInfo
import io.circe.generic.JsonCodec

@JsonCodec
case class RegisterUser(
    firstName: String,
    lastName: String,
    email: String,
    password: String
)
