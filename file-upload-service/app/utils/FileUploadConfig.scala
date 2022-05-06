package utils

import shared.config.MicroServiceConfig
import shared.auth.ApiKey
import shared.config.JwtConfig

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

case class FileUploadConfig(
    filePath: String,
    override val serviceUrl: String,
    override val apiKey: ApiKey,
    override val jwtConfig: JwtConfig
) extends MicroServiceConfig

object MicroServiceConfigLoader extends ConfigLoader[FileUploadConfig] {
  override def load(config: Config, path: String): FileUploadConfig =
    FileUploadConfig(
      config.getString("upload-service.filePath"),
      config.getString("app.api.url"),
      ApiKey(
        config.getString("app.api.name"),
        config.getString("app.api.key")
      ), {

        val jwt = config.getConfig("jwt")

        new JwtConfig {
          val alg: String          = jwt.getString("alg")
          val typ: String          = Try(jwt.getString("typ")).getOrElse("JWT")
          val secret: String       = jwt.getString("secret")
          val issuer: String       = jwt.getString("issuer")
          val expirationTime: Long = jwt.getString("expirationTime").toLong

        }
      }
    )
}
