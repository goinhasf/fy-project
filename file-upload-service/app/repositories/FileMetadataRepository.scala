package repositories

import org.mongodb.scala.MongoCollection
import io.FileMetadata
import scala.concurrent.Future
import com.mongodb.client.result.InsertOneResult
import org.mongodb.scala.model.Filters

trait FileMetadataRepository {
  def getFileMetadata(id: String): Future[Option[FileMetadata]]
  def saveMetadata(fileMetadata: FileMetadata): Future[String]
  def updateMetadata(
      id: String,
      fileMetadata: FileMetadata
  ): Future[Option[String]]
}

class FileMetadataRepositoryImpl(
    collection: MongoCollection[FileMetadata]
) extends FileMetadataRepository {
  override def getFileMetadata(id: String): Future[Option[FileMetadata]] =
    collection
      .find(Filters.eq("fileId", id))
      .headOption()

  override def saveMetadata(
      fileMetadata: FileMetadata
  ): Future[String] = collection
    .insertOne(fileMetadata)
    .map(_.getInsertedId().toString())
    .head()

  override def updateMetadata(
      id: String,
      newFileMetadata: FileMetadata
  ): Future[Option[String]] = collection
    .replaceOne(Filters.equal("fileId", id), newFileMetadata)
    .map(_ => id)
    .headOption()

}
