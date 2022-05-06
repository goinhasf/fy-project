package shared.auth

import io.circe.generic.JsonCodec

@JsonCodec
sealed trait UserCredential
@JsonCodec
case class Email(email: String) extends UserCredential
@JsonCodec
case class Password(salt: Salt, hash: Hash) extends UserCredential
@JsonCodec
case class Salt(value: String)
@JsonCodec
case class Hash(value: String, alg: String) {
  def compareString(string: String): Option[Hash] = Option
    .when(string == value)(this)
}
