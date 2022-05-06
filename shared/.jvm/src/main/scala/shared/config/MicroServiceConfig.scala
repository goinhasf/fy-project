package shared.config

import shared.auth.ApiKey

trait MicroServiceConfig {
  val serviceUrl: String
  val apiKey: ApiKey
  val jwtConfig: JwtConfig
}
