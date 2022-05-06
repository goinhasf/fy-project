import com.typesafe.config.ConfigFactory
import play.api.Environment
import play.api.ApplicationLoader
import play.api.Mode
import play.api.LoggerConfigurator
import play.api.Configuration
import play.core.server.ServerConfig
import server.AuthorizationServer
import server.AppContainer

object Main {
  // JVM entry point that starts the HTTP server
  def main(args: Array[String]): Unit = {

    val config      = ConfigFactory.defaultApplication().resolve()
    val environment = Environment.simple(mode = Mode.Dev)
    val context     = ApplicationLoader.Context.create(environment)

    // Do the logging configuration
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment, context.initialConfiguration, Map.empty)
    }

    val playConfig = new Configuration(config.getConfig("play"))
    val serverConfig = ServerConfig(
      port = playConfig
        .getOptional[Int]("server.http.port")
        .orElse(Some(9000)),
      mode = Mode.Dev
    )

    val authServer = new AuthorizationServer(
      serverConfig,
      components => new AppContainer(config, components)
    )
  }

  //#main-only
}
