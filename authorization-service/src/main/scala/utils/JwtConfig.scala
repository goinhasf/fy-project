package utils

import com.typesafe.config.Config
import pdi.jwt.JwtAlgorithm
import pdi.jwt.algorithms.JwtHmacAlgorithm
import scala.util.Try
import play.api.ConfigLoader
import pdi.jwt.JwtHeader
import shared.config.JwtConfig
import pdi.jwt.Jwt
import pdi.jwt.JwtClaim
import java.time.Clock

trait JwtConfigDependency {
  val jwtConfig: JwtConfig

}

object JwtConfigLoader extends ConfigLoader[JwtConfig] {

  override def load(config: Config, path: String): JwtConfig = {

    new JwtConfig {
      val alg: String          = config.getString("alg")
      val typ: String          = Try(config.getString("typ")).getOrElse("JWT")
      val secret: String       = config.getString("secret")
      val issuer: String       = config.getString("issuer")
      val expirationTime: Long = config.getString("expirationTime").toLong

    }
  }

}
