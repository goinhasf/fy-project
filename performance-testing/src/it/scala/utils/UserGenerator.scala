package utils

import dao.users.DefaultUserCredentials
import dao.users.User
import dao.users.UserInfo
import io.circe
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.bson.types.ObjectId
import org.mongodb.scala._
import org.mongodb.scala.model.Filters
import shared.auth.CommitteeMemberRole
import shared.auth.Email
import shared.auth.Privilege
import shared.auth.Role
import utils.TokenHashing._

import java.io.File
import java.io.PrintWriter
import java.util.concurrent.TimeUnit
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.Source
import scala.util.Random

object UserGenerator {
  val mongoClient = MongoClientLoader()

  val usersCollection = mongoClient
    .getDatabase(MongoClientLoader.mongoConfig.dbName)
    .getCollection("user")

  val random = new Random(9923)

  private def createRandomEmail =
    s"${random.alphanumeric.take(10).mkString}@user.com"

  def generate(prefix: String, n: Int): Seq[User] = {

    val users = Range(0, n).map { i =>
      val id = new ObjectId().toHexString()
      User(
        id,
        new UserInfo(
          id,
          random.alphanumeric.take(10).mkString,
          random.alphanumeric.take(10).mkString,
          createRandomEmail,
          Role.TestUser(
            Privilege.committeeAdminPrivileges("6091791034a6591bfd5fc079")
          )
        ),
        DefaultUserCredentials(
          Email(random.alphanumeric.take(8).mkString),
          "password".toPassword
        )
      )
    }

    Await.result(
      usersCollection
        .insertMany(users.map(u => Document(u.asJson.noSpaces)))
        .head(),
      Duration(10, TimeUnit.SECONDS)
    )

    users

  }

  def deleteAllTestUsers = Await.result(
    usersCollection
      .deleteMany(
        Filters.eq("userInfo.role.roleType._t", "TestRole")
      )
      .map(_.getDeletedCount())
      .head(),
    Duration(10, TimeUnit.SECONDS)
  )

  def readUsers = Await.result(
    usersCollection
      .find(
        Filters.eq("userInfo.role.roleType", "TestRole")
      )
      .collect()
      .map(
        _.map(_.toJson())
          .map(u => decode[User](u).fold(throw _, identity))
      )
      .head(),
    Duration(10, TimeUnit.SECONDS)
  )
}
