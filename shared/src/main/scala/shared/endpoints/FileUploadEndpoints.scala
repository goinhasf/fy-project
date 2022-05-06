package shared.endpoints

import endpoints4s.algebra
import io.FileMetadata
import shared.endpoints.authentication.Authentication

trait FileUploadEndpoints
    extends algebra.Endpoints
    with algebra.circe.JsonEntitiesFromCodecs
    with MultipartFormEndpoints
    with Authentication {

  val restPath = path / "api" / "file-upload"

  def uploadEndpoint: Endpoint[(FormData, AuthenticationToken), String] =
    endpoint(
      authenticatedRequest(
        Post,
        restPath,
        multipartFormRequestEntity,
        emptyRequestHeaders,
        None
      ),
      ok(textResponse)
    )

  def updateFileEndpoint
      : Endpoint[(String, FormData, AuthenticationToken), Option[String]] =
    endpoint(
      authenticatedRequest(
        Put,
        restPath / segment[String]("fileId"),
        multipartFormRequestEntity,
        emptyRequestHeaders,
        None
      ),
      ok(textResponse).orNotFound()
    )

  def getFileMetadata: Endpoint[(String, AuthenticationToken), Option[FileMetadata]] =
    endpoint(
      authenticatedRequest(
        Get,
        restPath / segment[String]("fileId"),
        emptyRequest,
        emptyRequestHeaders,
        None
      ),
      ok(jsonResponse[FileMetadata]).orNotFound()
    )

  def downloadFileEndpoint: Endpoint[String, Option[File]] = endpoint(
    get(restPath / "download" / segment[String]("fileId")),
    ok(fileResponseEntity).orNotFound()
  )

}
