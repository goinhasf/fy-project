package services

import java.time.Clock
import java.util.concurrent.TimeUnit

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Duration

import dao.users.UserInfo
import endpoints4s.play.server.PlayComponents
import io.jsonwebtoken.JwtException
import org.bson.types.ObjectId
import org.mockito.ArgumentMatchersSugar
import org.mockito.IdiomaticMockito
import pdi.jwt.JwtAlgorithm
import pdi.jwt.JwtClaim
import pdi.jwt.JwtHeader
import play.api.Configuration
import repositories.TokensRepository
import repositories.UsersRepository
import shared.auth.Role
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
import pdi.jwt.exceptions.JwtLengthException
import pdi.jwt.exceptions.JwtExpirationException
import shared.config.JwtConfig
import pdi.jwt.exceptions.JwtEmptyAlgorithmException
import shared.auth.UserCredentialsAuth
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import dao.users.User
import shared.auth.Token
import dao.users.DefaultUserCredentials
import shared.auth.Email
import shared.auth.Password
import utils.TokenHashing._
class AuthorizationServiceTestSuite
    extends AnyWordSpec
    with Matchers
    with IdiomaticMockito
    with ArgumentMatchersSugar {

  val usersRepository  = mock[UsersRepository]
  val tokensRepository = mock[TokensRepository]

  implicit val clock = Clock.systemUTC()

  val maxFutureResolveDuration = Duration(2, TimeUnit.SECONDS)

  val jwtConfig = new JwtConfig {
    val alg: String          = JwtAlgorithm.HS256.name
    val typ: String          = "JWT"
    val secret: String       = "secret"
    val issuer: String       = "test-app"
    val expirationTime: Long = 1000
  }

  val controllerComponents = mock[PlayComponents]
  val playConfiguration    = mock[Configuration]

  controllerComponents.executionContext returns ExecutionContext
    .Implicits
    .global

  def createFakeToken(
      user: User,
      expiresIn: Long = jwtConfig.expirationTime
  ): (JwtClaim, Token) = {
    val claim = JwtClaim(
      userInfo.asJson.noSpacesSortKeys,
      Some(jwtConfig.issuer)
    ).expiresIn(expiresIn)
    val fakeJwtToken = jwtConfig.encode(
      claim
    )
    (claim, Token(user._id, fakeJwtToken, new ObjectId().toHexString()))
  }

  val userId = new ObjectId().toHexString()
  val userInfo = UserInfo(
    userId,
    "Test",
    "Subject",
    "test@tester.com",
    Role.RegularUser(Set.empty)
  )

  val email    = "bobjones@test.com"
  val password = "awesome"

  val userCredentials = DefaultUserCredentials(
    Email(email),
    password.toPassword
  )

  val fakeUser = User(
    userId,
    userInfo,
    userCredentials
  )

  val authorizationService = new AuthorizationService(
    jwtConfig,
    controllerComponents,
    playConfiguration,
    usersRepository,
    tokensRepository
  )

  val (claim, fakeToken) = createFakeToken(fakeUser)

  "Calling createToken" should {
    "create a new token for a first time login" in {
      usersRepository
        .findUser(fakeUser.credentials.email.email)
        .returns(Future.successful(Some(fakeUser)))

      tokensRepository
        .findTokenBelongingTo(fakeUser.userInfo)
        .returns(Future.successful(None))

      tokensRepository
        .createNewTokenFor(fakeUser)
        .returns(Future.successful(fakeToken))

      Await.result(
        authorizationService
          .createTokenEndpoint
          .service(
            UserCredentialsAuth(
              email,
              password
            )
          ),
        maxFutureResolveDuration
      ) mustBe fakeToken._id

    }
  }

  "Calling authenticate" should {
    "return the valid token if the token is valid" in {

      tokensRepository
        .findTokenById(fakeToken._id)
        .returns(Future.successful(Some(fakeToken)))

      Await.result(
        authorizationService
          .authorizeEndpoint
          .service(fakeToken._id),
        maxFutureResolveDuration
      ) mustBe Some(fakeToken._id)

    }

    "return None on an invalid token" in {

      tokensRepository
        .findTokenById("someId")
        .returns(
          Future.successful(None)
        )

      assertResult(None) {
        Await
          .result(
            authorizationService.authorizeEndpoint.service("someId"),
            maxFutureResolveDuration
          )
      }

    }

    "return none an expired token" in {

      val (c, t) = createFakeToken(fakeUser, -10000)

      tokensRepository
        .findTokenById(t._id)
        .returns(
          Future.successful(Some(t))
        )

      assertResult(None) {
        Await
          .result(
            authorizationService
              .authorizeEndpoint
              .service(
                t._id
              ),
            maxFutureResolveDuration
          )
      }
    }
  }
}
