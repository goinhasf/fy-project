package server

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.MongoCredential
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import endpoints4s.play.server.PlayComponents
import io.netty.util.internal.logging.InternalLoggerFactory
import io.netty.util.internal.logging.Slf4JLoggerFactory
import org.mongodb.scala.MongoClient
import play.api.ApplicationLoader
import play.api.BuiltInComponents
import play.api.Configuration
import play.api.Environment
import play.api.Logger
import play.api.LoggerConfigurator
import play.api.Mode
import play.core.server.NettyServer
import play.core.server.ServerConfig
import repositories.ApiKeysRepository
import repositories.ApiKeysRepositoryImpl
import repositories.RepositoryDependencies
import repositories.TokensRepository
import repositories.UsersRepository
import services.AuthorizationService
import services.UsersService
import shared.auth.ApiKey
import utils.ConfigurationDependencies
import utils._
import play.api.routing.SimpleRouter
import play.api.routing.Router
import org.bson.codecs.configuration.CodecProvider

abstract class Container(config: Config, components: BuiltInComponents)
    extends SimpleRouter

case class AppContainer(config: Config, components: BuiltInComponents)
    extends Container(config, components)
    with ConfigurationDependencies
    with RepositoryDependencies {

  val logger = Logger(classOf[AppContainer])

  val playConfiguration = new Configuration(config.getConfig("play"))
  val keysConfig        = KeysConfig.configLoader.load(ConfigFactory.load("keys"), "")
  val mongoConfig = MongoConfigLoader
    .configLoader
    .load(config.getConfig("mongo"), "")
  val jwtConfig = JwtConfigLoader.load(config.getConfig("jwt"), "")
  val mongoClient = MongoClient(
    MongoClientSettings
      .builder()
      .applyConnectionString(new ConnectionString(mongoConfig.address))
      .credential(
        MongoCredential.createCredential(
          mongoConfig.username,
          mongoConfig.dbName,
          mongoConfig.password.toCharArray()
        )
      )
      .build()
  )

  val mongoDatabase = mongoClient
    .getDatabase(mongoConfig.dbName)
    .withCodecRegistry(MongoClient.DEFAULT_CODEC_REGISTRY)

  val usersRepository = new UsersRepository(
    mongoDatabase.getCollection(
      mongoConfig.collections(UsersCollection)
    )
  )

  val tokensRepository = new TokensRepository(
    mongoDatabase.getCollection(
      mongoConfig.collections(TokensCollection)
    ),
    jwtConfig
  )

  val apiKeysRepository = new ApiKeysRepositoryImpl(
    mongoDatabase.getCollection(
      mongoConfig.collections(ApiKeysCollection)
    )
  )

  val authorizationService =
    new AuthorizationService(
      jwtConfig,
      PlayComponents.fromBuiltInComponents(components),
      playConfiguration,
      usersRepository,
      tokensRepository
    )

  val usersService = new UsersService(
    jwtConfig,
    PlayComponents.fromBuiltInComponents(components),
    usersRepository,
    tokensRepository,
    apiKeysRepository
  )

  override val routes: Router.Routes =
    authorizationService.routes.orElse(usersService.routes)
}

class AuthorizationServer(
    serverConfig: ServerConfig,
    appContainer: BuiltInComponents => Container
) {

  val server = NettyServer.fromRouterWithComponents(serverConfig) {
    components => appContainer(components).routes
  }
}
