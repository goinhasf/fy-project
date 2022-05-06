package services

import java.io.File
import java.util.concurrent.TimeUnit

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Duration

import com.typesafe.config.ConfigFactory
import dao.users.UserInfo
import endpoints4s.play.server.PlayComponents
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import org.bson.types.ObjectId
import org.mockito.ArgumentMatchersSugar
import org.mockito.IdiomaticMockito
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import play.api.ApplicationLoader
import play.api.Configuration
import play.api.Environment
import play.api.LoggerConfigurator
import play.api.Mode
import play.api.http.ContentTypes
import play.api.routing.Router
import play.api.test.WsTestClient
import play.core.server.ServerConfig
import repositories.ApiKeysRepository
import repositories.UsersRepository
import server.AppContainer
import server.AuthorizationServer
import server.Container
import shared.auth.ApiKey
import shared.auth.Role
import shared.endpoints.user.AddPrivilegesPayload
import shared.auth.Privilege
import repositories.TokensRepository
import utils.JwtConfigLoader
import dao.users.User
import dao.users.DefaultUserCredentials
import shared.auth.Email
import utils.TokenHashing._
import shared.auth.Token
class UsersServiceTest
    extends AnyFunSpec
    with Matchers
    with IdiomaticMockito
    with ArgumentMatchersSugar {

  implicit val ec   = ExecutionContext.global
  implicit val port = 9111

  val config      = ConfigFactory.load()
  val playConfig  = new Configuration(config.getConfig("play"))
  val environment = Environment.simple(mode = Mode.Test)
  val context     = ApplicationLoader.Context.create(environment)

  // Do the logging configuration
  LoggerConfigurator(context.environment.classLoader).foreach {
    _.configure(context.environment, context.initialConfiguration, Map.empty)
  }
  val serverConfig = ServerConfig(
    port = Some(port),
    mode = Mode.Test
  )
  val userId = new ObjectId().toHexString()
  val user: User = User(
    userId,
    UserInfo(
      userId,
      "Test",
      "Subject",
      "test@tester.com",
      Role.RegularUser(Set.empty)
    ),
    DefaultUserCredentials(
      Email("test@tester.com"),
      "password".toPassword
    )
  )
  val apiKey            = ApiKey("test-client", "abc")
  val payload           = AddPrivilegesPayload(apiKey, Role.Admin().privileges)
  val usersRepository   = mock[UsersRepository]
  val apiKeysRepository = mock[ApiKeysRepository]
  val tokensRepository  = mock[TokensRepository]

  usersRepository
    .findUserById(userId)
    .returns(Future.successful(Some(user)))
  apiKeysRepository
    .findApiKey(apiKey)
    .returns(Future.successful(Some(apiKey)))

  val application = new AuthorizationServer(
    serverConfig,
    comp =>
      new Container(config, comp) {
        def routes: Router.Routes = new UsersService(
          JwtConfigLoader.load(config.getConfig("jwt"), ""),
          PlayComponents.fromBuiltInComponents(comp),
          usersRepository,
          tokensRepository,
          apiKeysRepository
        ).routes
      }
  )
  it("should update the role of the user") {

    usersRepository
      .updatePrivileges(userId, Role.Admin().privileges)
      .returns(
        Future.successful(
          Some(
            User(
              user._id,
              UserInfo(
                user.userInfo.id,
                user.userInfo.firstName,
                user.userInfo.lastName,
                user.userInfo.email,
                Role.Admin()
              ),
              user.credentials
            )
          )
        )
      )

    tokensRepository
      .updateToken[UserInfo](any, any)
      .returns(
        Future.successful(
          Some(Token(user._id, "encoded.encoded.encoded", "tokenId"))
        )
      )

    val response = WsTestClient.withClient(client =>
      client
        .url(s"http://localhost:$port/api/user/role?userId=${user.userInfo.id}")
        .withHttpHeaders("Content-Type" -> ContentTypes.JSON)
        .post(payload.asJson.noSpaces)
        .map(_.body)
    )

    val result = Await.result(response, Duration(5, TimeUnit.SECONDS))

    usersRepository.findUserById(userId) wasCalled 1.times
    apiKeysRepository.findApiKey(apiKey) wasCalled 1.times
    usersRepository.updatePrivileges(
      user.userInfo.id,
      Role.Admin().privileges
    ) wasCalled 1.times

    decode[UserInfo](result) match {
      case Left(value) => fail("Could not decode result", value)
      case Right(value) =>
        value.role.roleType shouldEqual (Role.Admin().roleType)
    }
  }

  it("should return 404 if user is not found") {

    reset(usersRepository, apiKeysRepository)

    val wrongUserId = "abc"

    usersRepository
      .findUserById(wrongUserId)
      .returns(Future.successful(None))

    apiKeysRepository
      .findApiKey(apiKey)
      .returns(Future.successful(Some(apiKey)))

    usersRepository
      .updatePrivileges(userId, Role.Admin().privileges)
      .returns(
        Future.successful(
          None
        )
      )

    val response = WsTestClient.withClient(client =>
      client
        .url(s"http://localhost:$port/api/user/role?userId=$wrongUserId")
        .withHttpHeaders("Content-Type" -> ContentTypes.JSON)
        .post(payload.asJson.noSpaces)
    )

    val result = Await.result(response, Duration(5, TimeUnit.SECONDS))

    usersRepository.findUserById(wrongUserId) wasCalled 1.times
    apiKeysRepository.findApiKey(apiKey) wasCalled 1.times
    usersRepository.updatePrivileges(
      user.userInfo.id,
      Role.Admin().privileges
    ) wasCalled 0.times

    result.status shouldEqual 404
  }

}
