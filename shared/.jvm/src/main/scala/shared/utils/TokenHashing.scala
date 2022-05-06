package utils

import java.security.MessageDigest
import java.security.SecureRandom

import io.circe.generic.semiauto._
import shared.auth.{Email, Hash, Password, Salt}

object TokenHashing {
  val algorithm = "SHA-256"

  implicit final class TextToEmail(text: String) {
    def toEmail: Option[Email] = Some(Email(text))
  }

  implicit final class ClearTextToPassword(text: String) {
    def toPassword: Password = {
      val salt = TokenHashing.create32ByteSalt()
      Password(
        salt,
        TokenHashing.createHashFromString(salt.value + text, algorithm)
      )
    }

  }

  implicit final class ComparePassword(pass: Password) {
    def compareString(string: String): Option[Password] =
      createHashFromString(pass.salt.value + string, algorithm)
        .compareString(pass.hash.value)
        .map(_ => pass)
  }

  def create32ByteSalt(): Salt = {
    var arr = new Array[Byte](32)
    new SecureRandom().nextBytes(arr)
    Salt(arr.map("%02x".format(_)).mkString)
  }

  def createHashFromString(string: String, alg: String): Hash = {
    Hash(
      MessageDigest
        .getInstance(alg)
        .digest(string.getBytes("UTF-8"))
        .map("%02x".format(_))
        .mkString,
      alg
    )
  }

}
