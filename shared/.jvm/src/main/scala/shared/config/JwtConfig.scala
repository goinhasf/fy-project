package shared.config

import pdi.jwt.JwtAlgorithm
import pdi.jwt.JwtHeader
import scala.util.Try
import java.time.Clock
import pdi.jwt.JwtClaim
import pdi.jwt.Jwt

trait JwtConfig {
  val alg: String
  val typ: String
  val secret: String
  val issuer: String
  val expirationTime: Long

  private def algorithm = JwtAlgorithm
    .allHmac()
    .find(_.name == alg)

  def getHeader(): JwtHeader = JwtHeader(algorithm.get, typ)

  def decode(value: String): Try[JwtClaim] = {
    implicit val clock = Clock.systemUTC()
    Jwt.decode(
      value,
      secret,
      algorithm.toList
    )
  }

  def encode(jwtClaim: JwtClaim): String = Jwt.encode(
    getHeader(),
    jwtClaim,
    secret
  )
}
