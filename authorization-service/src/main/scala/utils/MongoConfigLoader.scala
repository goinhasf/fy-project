package utils

import com.typesafe.config.Config
import play.api.ConfigLoader
import shared.config.MongoConfig

trait MongoConfigDependency {
  val mongoConfig: MongoConfig[UserDBCollection]
}
object MongoConfigLoader {
  lazy val configLoader = new ConfigLoader[MongoConfig[UserDBCollection]] {
    override def load(
        config: Config,
        path: String
    ): MongoConfig[UserDBCollection] = {
      val dbName: String = config.getString("dbName")
      val collections: Map[UserDBCollection, String] = Map(
        UsersCollection  -> config.getString("collections.users"),
        TokensCollection -> config.getString("collections.tokens"),
        RolesCollection  -> config.getString("collections.roles"),
        ApiKeysCollection -> config.getString("collections.apiKeys")
      )
      val username: String = config.getString("username")
      val password: String = config.getString("pwd")
      val address: String  = config.getString("address")

      MongoConfig(
        address,
        dbName,
        collections,
        username,
        password
      )
    }

  }
}

sealed trait UserDBCollection
object UsersCollection   extends UserDBCollection
object TokensCollection  extends UserDBCollection
object RolesCollection   extends UserDBCollection
object ApiKeysCollection extends UserDBCollection
