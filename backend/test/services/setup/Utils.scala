package services.setup

import org.mongodb.scala._

object Utils {
  lazy val getMongoClient = MongoClient(
    MongoClientSettings
      .builder()
      .credential(
        MongoCredential.createScramSha256Credential(
          "tester",
          "admin",
          "password".toCharArray()
        )
      )
      .build()
  )
}
