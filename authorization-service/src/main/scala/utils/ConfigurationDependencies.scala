package utils

import endpoints4s.play.server.PlayComponents
import play.api.Configuration

trait ConfigurationDependencies
    extends JwtConfigDependency
    with KeysConfigDependency
    with MongoConfigDependency {
  val playConfiguration: Configuration
}
