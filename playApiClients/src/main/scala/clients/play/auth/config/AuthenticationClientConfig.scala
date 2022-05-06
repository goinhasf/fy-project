package clients.play.auth.config

import shared.config.MicroServiceConfig

trait AuthenticationClientConfig extends MicroServiceConfig {
    val cookieName: String
}