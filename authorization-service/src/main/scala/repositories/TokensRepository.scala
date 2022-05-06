package repositories

import java.time.Clock

import scala.concurrent.Future

import dao.users.User
import shared.auth.Token
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import org.mongodb.scala._
import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.Updates._
import pdi.jwt.JwtClaim
import pdi.jwt.JwtHeader
import dao.users.UserInfo
import pdi.jwt.JwtAlgorithm
import io.circe.parser._, io.circe.syntax._
import scala.util.Try
import shared.config.JwtConfig
import pdi.jwt.JwtTime
import io.circe.Encoder

class TokensRepository(
    tokensCollection: MongoCollection[Document],
    jwtConfig: JwtConfig
) {

  implicit val clock = Clock.systemUTC()

  def findTokenById(id: String): Future[Option[Token]] = {

    tokensCollection
      .find(
        Filters.equal("_id", id)
      )
      .map(_.toJson())
      .map(decode[Token](_).fold(throw _, identity))
      .headOption()

  }

  def findTokenBelongingTo(user: UserInfo): Future[Option[Token]] =
    tokensCollection
      .find(Filters.equal[ObjectId]("_userId", new ObjectId(user.id)))
      .map(_.toJson())
      .map(decode[Token](_).fold(throw _, identity))
      .headOption()

  def createNewTokenFor(user: User): Future[Token] = {

    val tokenId = new ObjectId().toHexString()
    val jwtClaim = JwtClaim(issuer = Some(jwtConfig.issuer))
      .expiresIn(jwtConfig.expirationTime)
      .withContent(user.userInfo.asJson.noSpacesSortKeys)
      .withId(tokenId)

    val jwtJson = jwtConfig.encode(jwtClaim)

    val newToken = Token(
      user._id,
      jwtJson,
      tokenId
    )

    tokensCollection
      .deleteMany(Filters.eq("_userId", user._id))
      .flatMap(_ =>
        tokensCollection
          .insertOne(Document(newToken.asJson.noSpaces))
          .map(_ => newToken)
      )
      .head()

  }

  def decodeTokenString(token: String): Option[UserInfo] = {
    jwtConfig
      .decode(token)
      .toOption
      .flatMap { claim =>
        decode[UserInfo](claim.content).toOption
      }
  }

  def deleteToken(token: Token): Future[Long] = {
    tokensCollection
      .deleteOne(Filters.eq("_id", token._id))
      .map(_.getDeletedCount())
      .head()
  }

  def updateToken[T](user: UserInfo, tokenVal: T)(implicit
      encoder: Encoder[T]
  ): Future[Option[Token]] = {

    tokensCollection
      .find(Filters.equal("_userId", user.id))
      .map(_.toJson())
      .map(decode[Token](_).fold(throw _, identity))
      .flatMap { token =>
        val claim = JwtClaim(issuer = Some(jwtConfig.issuer))
          .expiresIn(jwtConfig.expirationTime)
          .withContent(tokenVal.asJson.noSpacesSortKeys)
          .withId(token._id)

        tokensCollection
          .findOneAndUpdate(
            Filters.equal("_userId", user.id),
            set("encodedValue", jwtConfig.encode(claim))
          )
      }
      .map(_.toJson())
      .map(decode[Token](_).fold(throw _, identity))
      .headOption()
  }

}
