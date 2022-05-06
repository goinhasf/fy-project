package utils

import com.typesafe.config.Config
import play.api.ConfigLoader
import shared.config.MongoConfig

object MongoConfigLoader
    extends ConfigLoader[MongoConfig[FileUploadDBCollections]] {

  override def load(
      config: Config,
      path: String
  ): MongoConfig[FileUploadDBCollections] = {
    val dbName: String = config.getString("dbName")
    val collections: Map[FileUploadDBCollections, String] = Map(
      FileMetadataCollection -> config.getString("collections.fileMetadata")
    )
    val address: String  = config.getString("address")
    val username: String = config.getString("username")
    val password: String = config.getString("pwd")

    MongoConfig(
      address,
      dbName,
      collections,
      username,
      password
    )
  }

}

sealed trait FileUploadDBCollections
object FileMetadataCollection  extends FileUploadDBCollections
