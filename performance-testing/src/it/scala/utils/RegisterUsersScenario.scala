package utils

import scala.concurrent.duration._
import io.circe
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import utils.UserGenerator
import scala.io.Source
import shared.auth.UserCredentialsAuth

object RegisterUsersScenario {

  case class ApiKey(serviceId: String, apiKey: String)

  val apiKey =
    decode[ApiKey](
      Source.fromFile("src/it/resources/secret/apiKey.json").mkString
    ) match {
      case Left(value)  => throw value
      case Right(value) => value
    }

  def createUsersFeeder(n: Int) = {
    UserGenerator.deleteAllTestUsers

    Iterator.from(
      UserGenerator
        .generate("user", n)
        .map(u => Map("username" -> u.credentials.email.email))
    )
  }
}
