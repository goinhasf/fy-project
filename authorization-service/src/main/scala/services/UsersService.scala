package services

import shared.config.JwtConfig
import endpoints4s.play.server.PlayComponents
import play.api.Configuration
import repositories.UsersRepository
import play.api.routing.SimpleRouter
import play.api.routing.Router
import shared.endpoints.user.{AddPrivilegesPayload, UserPrivilegeEndpoints}
import endpoints4s.play.server
import scala.concurrent.Future
import dao.users.UserInfo
import repositories.ApiKeysRepository
import shared.auth.ApiKey
import shared.auth.Role
import endpoints4s.Invalid
import play.api.Logger
import scala.util.Failure
import scala.util.Success
import shared.auth.AdminRole
import shared.auth.GuildAdminRole
import shared.auth.RegularUserRole
import shared.auth.CommitteeMemberRole
import shared.auth.CommitteeAdminRole
import shared.auth.Privilege
import repositories.TokensRepository
import shared.endpoints.UserInfoEndpoints
import endpoints4s.Tupler
import pdi.jwt.JwtClaim
import play.api.mvc.Results
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import scala.concurrent.ExecutionContext
import play.api.mvc.BodyParser
import endpoints4s.Valid
import play.api.http.HeaderNames
import play.api.libs.streams.Accumulator
import io.circe.parser._
import shared.endpoints.authentication.UserManagementEndpoints
import dao.users.User
import org.bson.types.ObjectId
import dao.users.DefaultUserCredentials
import shared.auth.Email
import shared.auth.Password
import utils.TokenHashing._

class UsersService(
    jwtConfig: JwtConfig,
    override val playComponents: PlayComponents,
    usersRepository: UsersRepository,
    tokensRepository: TokensRepository,
    apiKeysRepository: ApiKeysRepository
) extends SimpleRouter
    with server.Endpoints
    with server.JsonEntitiesFromCodecs
    with UserPrivilegeEndpoints
    with UserInfoEndpoints
    with UserManagementEndpoints {

  implicit val ec: ExecutionContext = playComponents.executionContext
  case class AuthenticationToken(tokenId: String)

  def authenticationToken[A](implicit
      request: Request[A]
  ): Response[AuthenticationToken] = resp => Results.Ok(resp.tokenId)

  protected def authenticatedRequest[U, E, H, UE, HCred, Out](
      method: Method,
      url: Url[U],
      entity: RequestEntity[E],
      headers: RequestHeaders[H],
      requestDocs: Option[String]
  )(implicit
      tuplerUE: Tupler.Aux[U, E, UE],
      tuplerHCred: Tupler.Aux[
        H,
        AuthenticationToken,
        HCred
      ],
      tuplerUEHCred: Tupler.Aux[UE, HCred, Out]
  ): Request[Out] = {
    val authenticationTokenRequestHeaders
        : RequestHeaders[Option[AuthenticationToken]] = { headers =>
      Valid(
        headers
          .get(HeaderNames.AUTHORIZATION)
          .map { headerValue =>
            AuthenticationToken(headerValue.stripPrefix("Bearer "))
          } match {
          case Some(token) => Some(token)
          case None        => None
        }
      )
    }
    extractMethodUrlAndHeaders(
      method,
      url,
      headers ++ authenticationTokenRequestHeaders
    )
      .toRequest[Out] {
        case (_, (headers, None)) =>
          _ =>
            Some(
              BodyParser(_ => Accumulator.done(Left(Results.Unauthorized(""))))
            )
        case (u, (headers, Some(token))) =>
          h =>
            entity(h).map(
              _.map(e =>
                tuplerUEHCred(tuplerUE(u, e), tuplerHCred(headers, token))
              )
            )
      } { out =>
        val (ue, hCred) = tuplerUEHCred.unapply(out)
        val (u, _)      = tuplerUE.unapply(ue)
        val (h, b)      = tuplerHCred.unapply(hCred)
        (u, (h, Some(b)))
      }

  }

  protected def wheneverAuthenticated[A, B](response: Response[B])(implicit
      request: Request[A]
  ): Response[B] = response

  val logger = Logger(classOf[UsersService])

  val routes: Router.Routes = routesFromEndpoints(
    addPrivileges.implementedByAsync(updateRoleService.tupled),
    getUserInfoEndpoint,
    getUserInfoApiKeyEndpoint,
    registerEndpoint
  )

  def updateRoleService = (
      userId: String,
      payload: AddPrivilegesPayload
  ) => {
    val future = for {
      userOpt   <- usersRepository.findUserById(userId)
      apiKeyOpt <- apiKeysRepository.findApiKey(payload.apiKey)
    } yield {

      if (apiKeyOpt.isEmpty) {
        Future.successful(Left(Invalid("Incorrect Api Key")))
      } else {
        val maybeUpdate = userOpt.map(_.userInfo).map { user =>
          val newPrivileges = user.role.privileges ++ payload.newPrivileges
          usersRepository
            .updatePrivileges(user.id, newPrivileges)
            .map(_.map(_.userInfo))
            .flatMap(_ match {
              case Some(value) =>
                tokensRepository.updateToken(user, value).map(_.map(_ => value))
              case None => Future.successful(None)
            })
        }

        maybeUpdate match {
          case None        => Future.successful(Right(None))
          case Some(value) => value.map(Right(_))
        }
      }

    }
    future.flatMap(identity)
  }

  def getUserInfoApiKeyEndpoint = getUserInfoApiKey.implementedByAsync { args =>
    val (id, serviceId, apiKey) = args
    apiKeysRepository
      .findApiKey(ApiKey(serviceId, apiKey))
      .collect({ case Some(value) => value })
      .recoverWith({ case _: NoSuchElementException =>
        Future.failed(new Exception("Invalid Api key"))
      })
      .flatMap(_ =>
        usersRepository.findUserById(args._1).map(_.map(_.userInfo))
      )
  }

  def getUserInfoEndpoint = getUserInfo.implementedByAsync { args =>
    tokensRepository
      .findTokenById(args._2.tokenId)
      .collect({ case Some(value) => value })
      .map(t =>
        jwtConfig
          .decode(t.encodedValue)
          .map(_.content)
          .toEither
          .flatMap(decode[UserInfo](_)) match {
          case Left(exception) => throw exception
          case Right(value)    => value
        }
      )
      .recoverWith({ case _: NoSuchElementException =>
        Future.failed(new Exception("Invalid token"))
      })
      .filter(userInfo => {
        userInfo.role.roleType == AdminRole() || userInfo
          .role
          .roleType == GuildAdminRole()
      })
      .recoverWith({ case _: NoSuchElementException =>
        Future.failed(new Exception("User is not authorized on this endpoint"))
      })
      .flatMap(_ =>
        usersRepository.findUserById(args._1).map(_.map(_.userInfo))
      )

  }

  def registerEndpoint = register.implementedByAsync { cred =>
    val id = new ObjectId()
    usersRepository
      .findUser(cred.email)
      .flatMap(_ match {
        case Some(value) =>
          Future.failed(new IllegalArgumentException("User already exists"))
        case None =>
          usersRepository.createNewUser(
            User(
              id.toHexString(),
              UserInfo(
                id.toHexString(),
                cred.firstName,
                cred.lastName,
                cred.email,
                Role.RegularUser(Set())
              ),
              DefaultUserCredentials(
                cred.email.toEmail.get,
                cred.password.toPassword
              )
            )
          )
      })

  }

  def deleteUserEndpoint = remove.implementedByAsync { args =>
    val (id, serviceId, apiKey) = args

    apiKeysRepository
      .findApiKey(ApiKey(serviceId, apiKey))
      .collect({ case Some(value) => value })
      .flatMap(_ =>
        usersRepository
          .deleteUser(id)
          .map(
            _.filter(_ == 1)
              .map(_ => ())
          )
      )
  }

}
