package utils

import org.bson.types.ObjectId
import org.mongodb.scala._
import org.mongodb.scala.model.Filters
import scala.io.Source
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

case class MongoConfig(
    address: String,
    dbName: String,
    username: String,
    pwd: String
)

object MongoClientLoader {
  val json = Source
    .fromFile("src/it/resources/secret/mongoConfig.json")
    .mkString
  val mongoConfig = decode[MongoConfig](json) match {
    case Left(value)  => throw value
    case Right(value) => value
  }

  lazy val mongoClient = MongoClient(
    MongoClientSettings
      .builder()
      .applyConnectionString(
        new ConnectionString("mongodb://" + mongoConfig.address)
      )
      .credential(
        MongoCredential.createCredential(
          mongoConfig.username,
          mongoConfig.dbName,
          mongoConfig.pwd.toCharArray()
        )
      )
      .build()
  )

  def apply() = mongoClient
}
