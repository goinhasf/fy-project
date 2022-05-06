import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.google.inject.AbstractModule
import com.mongodb.MongoClientSettings
import com.mongodb.MongoCredential
import com.typesafe.config.ConfigFactory
import io.FileMetadata
import org.bson.codecs.configuration.CodecRegistries.fromProviders
import org.bson.codecs.configuration.CodecRegistries.fromRegistries
import org.mongodb.scala.MongoClient
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.MongoDatabase
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.model.Indexes
import play.api.Logger
import repositories.FileMetadataRepository
import repositories.FileMetadataRepositoryImpl
import org.mongodb.scala.bson.codecs.Macros
import com.mongodb.ConnectionString
import utils._
import shared.config.MongoConfig
import shared.config.MicroServiceConfig

class Module extends AbstractModule {

  val config = ConfigFactory
    .defaultApplication()
    .resolve()

  val logger: Logger = Logger("application")

  val mongoConfig: MongoConfig[FileUploadDBCollections] =
    MongoConfigLoader.load(config.getConfig("mongo"), "")

  val microServiceConfig: FileUploadConfig =
    MicroServiceConfigLoader.load(config)

  override protected def configure(): Unit = {
    val mongoClient = MongoClient(
      MongoClientSettings
        .builder()
        .credential(
          MongoCredential.createCredential(
            mongoConfig.username,
            mongoConfig.dbName,
            mongoConfig.password.toCharArray()
          )
        )
        .applyConnectionString(new ConnectionString(mongoConfig.address))
        .build()
    )

    val codecRegistry = fromProviders(
      createCodecProvider[FileMetadata](),
      DEFAULT_CODEC_REGISTRY
    )

    val db = mongoClient
      .getDatabase(mongoConfig.dbName)
      .withCodecRegistry(
        fromProviders(
          Macros.createCodecProvider[FileMetadata](),
          DEFAULT_CODEC_REGISTRY
        )
      )

    bind(classOf[MongoDatabase]).toInstance(db)
    bind(classOf[FileMetadataRepository]).toInstance(
      new FileMetadataRepositoryImpl(
        db.getCollection(mongoConfig.collections(FileMetadataCollection))
      )
    )
    bind(classOf[FileUploadConfig]).toInstance(microServiceConfig)
    bind(classOf[MicroServiceConfig]).toInstance(microServiceConfig)

  }

}
