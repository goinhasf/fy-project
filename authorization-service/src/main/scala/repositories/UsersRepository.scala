package repositories

import scala.concurrent.Future
import dao.users._
import org.mongodb.scala._
import org.mongodb.scala.model.Filters
import io.circe.parser._
import io.circe.syntax._
import shared.auth.Email
import shared.auth.Privilege
import org.mongodb.scala.model.Updates
import org.mongodb.scala.model.FindOneAndUpdateOptions
import org.mongodb.scala.model.ReturnDocument

class UsersRepository(usersCollection: MongoCollection[Document]) {

  def findUser(
      username: String
  ): Future[Option[User]] = {
    usersCollection
      .find(
        Filters
          .equal("credentials.email", Document(Email(username).asJson.noSpaces))
      )
      .map(_.toJson())
      .map(decode[User](_).fold(throw _, identity))
      .headOption()
  }

  def findUserById(
      userId: String
  ): Future[Option[User]] = usersCollection
    .find(
      Filters.equal("_id", userId)
    )
    .map(_.toJson())
    .map(decode[User](_).fold(throw _, identity))
    .headOption()

  def createNewUser(
      user: User
  ): Future[String] = {

    usersCollection
      .insertOne(Document(user.asJson.noSpaces))
      .map(_ => user._id)
      .head()

  }

  def deleteUser(id: String): Future[Option[Long]] = usersCollection
    .deleteOne(Filters.equal("_id", id))
    .map(_.getDeletedCount())
    .headOption()

  def updatePrivileges(
      userId: String,
      privileges: Set[Privilege]
  ): Future[Option[User]] = {

    usersCollection
      .find(Filters.equal("_id", userId))
      .map(_.toJson())
      .map(decode[User](_).fold(throw _, identity))
      .flatMap(user =>
        usersCollection
          .findOneAndUpdate(
            Filters.equal("_id", userId),
            Updates.set(
              "userInfo.role",
              Document(
                "roleType" -> Document(
                  user.userInfo.role.roleType.asJson.noSpaces
                ),
                "privileges" -> privileges
                  .map(_.asJson.noSpaces)
                  .map(Document(_))
                  .toSeq
              )
            ),
            FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
          )
          .map(_.toJson())
          .map(decode[User](_).fold(throw _, identity))
      )
      .headOption()
  }

}
