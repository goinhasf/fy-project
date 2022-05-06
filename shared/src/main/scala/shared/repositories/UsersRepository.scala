package shared.repositories

import scala.concurrent.Future

trait UsersRepository[User] {
  def findUser(userId: String): Future[User]
  def insertUser(userInfo: User): Future[String]
  def updateUserInfo(userId: String, newUserInfo: User): Future[Int]
}
