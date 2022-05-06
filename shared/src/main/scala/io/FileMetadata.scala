package io
import io.circe.generic.semiauto._

case class FileMetadata(
    fileId: String,
    fileName: String,
    size: Long,
    contentType: Option[String]
)
object FileMetadata {
  implicit val codec = deriveCodec[FileMetadata]
}
