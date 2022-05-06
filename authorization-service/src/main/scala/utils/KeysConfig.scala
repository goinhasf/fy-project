package utils

import play.api.ConfigLoader
import com.typesafe.config.Config

trait KeysConfigDependency {
  val keysConfig: KeysConfig
}

case class KeysConfig(
    apiKey: String
)
object KeysConfig {
  val configLoader = new ConfigLoader[KeysConfig] {
    override def load(config: Config, path: String): KeysConfig = KeysConfig(
      apiKey = config.getString("api-key")
    )
  }
}
