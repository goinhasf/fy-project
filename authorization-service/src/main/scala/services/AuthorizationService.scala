package services

import javax.security.auth.login.CredentialException
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util._
import utils.TokenHashing._
import com.google.inject._
import dao.users.User
import shared.auth.Token
import endpoints4s.play.server
import endpoints4s.play.server.PlayComponents
import pdi.jwt._
import pdi.jwt.exceptions.JwtValidationException
import play.api.Configuration
import play.api.Logger
import play.api.routing.Router
import play.api.routing.SimpleRouter
import repositories.TokensRepository
import repositories.UsersRepository
import shared.endpoints.authentication.AuthenticationEndpoints
import shared.endpoints.authorization.AuthorizationEndpoints
import utils.JwtConfigDependency
import utils.JwtConfigDependency
import utils.ObjectIdFormatJsonMacro
import java.time.Clock
import shared.config.JwtConfig

import dao.users.UserInfo

class AuthorizationService(
    override val jwtConfig: JwtConfig,
    override val playComponents: PlayComponents,
    override val playConfiguration: Configuration,
    val usersRepository: UsersRepository,
    val tokensRepository: TokensRepository
) extends ServerAuthorization
    with SimpleRouter {

  val logger                  = Logger(classOf[AuthorizationService])
  implicit val objectIdFormat = ObjectIdFormatJsonMacro

  val authorizeEndpoint = authorize.implementedByAsync { tokenId =>
    tokensRepository
      .findTokenById(tokenId)
      .map(tokenOpt =>
        tokenOpt.flatMap(token => {
          val decoded = jwtConfig.decode(token.encodedValue)
          decoded match {
            case Failure(exception) => {
              logger.debug("Invalid token", exception)
              None
            }
            case Success(value) => Some(token._id)
          }
        })
      )
  }

  val getTokenContentEndpoint = getTokenContent.implementedByAsync { tokenId =>
    tokensRepository
      .findTokenById(tokenId)
      .map(tokenOpt =>
        tokenOpt.flatMap(token => {
          val decoded = jwtConfig.decode(token.encodedValue)
          decoded match {
            case Failure(exception) => {
              logger.debug("Invalid token", exception)
              None
            }
            case Success(value) => Some(value.content)
          }
        })
      )
  }

  val createTokenEndpoint = createToken.implementedByAsync { credentials =>
    logger.debug(s"Creating new auth token for ${credentials.username}")
    usersRepository
      .findUser(credentials.username)
      .flatMap {
        _ match {
          case Some(user) => {
            Future.successful(
              user
                .credentials
                .password
                .compareString(credentials.password)
                .map(_ => user)
            )
          }
          case None => {
            logger.debug("Credentials not found")
            Future.failed(new CredentialException("Username not found"))
          }
        }
      }
      .flatMap {
        _ match {
          case Some(user) => findTokenOrCreateNew(user).map(_._id)
          case None => {
            logger.debug("Password incorrect")
            Future.failed(new CredentialException("Password was incorrect"))
          }
        }
      }

  }

  private def findTokenOrCreateNew(
      user: User
  ): Future[Token] = tokensRepository
    .findTokenBelongingTo(user.userInfo)
    .flatMap {
      _.fold(tokensRepository.createNewTokenFor(user))(handleTokenFound(user))
    }

  private def handleTokenFound(user: User)(token: Token) = {
    val decoded = jwtConfig
      .decode(token.encodedValue)
      .filter(_.isValid)
    decoded match {
      case Failure(exception) => {
        logger.info(exception.getMessage())
        tokensRepository
          .deleteToken(token)
          .flatMap(_ => tokensRepository.createNewTokenFor(user))
      }
      case Success(value) => Future.successful(token)
    }
  }

  private def validateTokenOpt(
      tokenOpt: Option[Token]
  ): Future[Token] = tokenOpt match {
    case Some(token) => {
      val decoded = jwtConfig.decode(token.encodedValue)
      decoded match {
        case Failure(exception) => Future.failed(exception)
        case Success(value) => {
          if (value.isValid) {
            Future.successful(token)
          } else {
            Future.failed(new Throwable("Tokens is no longer valid"))
          }
        }
      }
    }
    case None => Future.failed[Token](new Exception("Token Not Found"))
  }

  override def routes: Router.Routes = routesFromEndpoints(
    authorizeEndpoint,
    createTokenEndpoint,
    getTokenContentEndpoint
  )

}
