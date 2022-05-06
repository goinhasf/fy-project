package dao.users

import shared.auth.Email
import shared.auth.Password
import shared.auth.UserCredential
import io.circe.generic.JsonCodec
import dao.users.UserInfo

@JsonCodec
case class DefaultUserCredentials(email: Email, password: Password)

@JsonCodec
case class User(
    _id: String,
    userInfo: UserInfo,
    credentials: DefaultUserCredentials
)
