package utils

import play.api.ConfigLoader
import shared.config.MicroServiceConfig
import com.typesafe.config.Config
import shared.config.JwtConfig
import pdi.jwt.JwtHeader
import scala.util.Try
import pdi.jwt.JwtClaim
import java.time.Clock
import pdi.jwt.Jwt
import pdi.jwt.JwtAlgorithm
import play.api.Configuration
import shared.auth.ApiKey

object MicroServiceConfigLoader extends ConfigLoader[MicroServiceConfig] {
  override def load(config: Config, path: String): MicroServiceConfig =
    new MicroServiceConfig {
      val serviceUrl: String = config.getString("app.api.url")
      val apiKey: ApiKey = ApiKey(
        config.getString("app.api.name"),
        config.getString("app.api.key")
      )

      private val jwt = config.getConfig("jwt")

      val jwtConfig: JwtConfig = new JwtConfig {
        val alg: String          = jwt.getString("alg")
        val typ: String          = Try(jwt.getString("typ")).getOrElse("JWT")
        val secret: String       = jwt.getString("secret")
        val issuer: String       = jwt.getString("issuer")
        val expirationTime: Long = jwt.getString("expirationTime").toLong

      }
    }
}
