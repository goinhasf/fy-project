package shared.repositories

import scala.concurrent.Future

trait TokensRepository[Token] {
  def findTokenById(tokenId: String): Future[Token]
  def findTokenByValue(tokenValue: String): Future[Token]
  def insertToken(token: Token): Future[String]
  def deleteToken(tokenId: String): Future[Int]
  def updateToken(tokenId: String, newTokenValue: Token): Future[Int]
}
