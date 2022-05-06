package controllers

import endpoints4s.openapi
import endpoints4s.openapi.model.{Info, OpenApi}
import endpoints4s.play.server
import shared.endpoints.FileUploadEndpoints
import endpoints4s.openapi.model.MediaType
import endpoints4s.openapi.model.Schema.Primitive
import endpoints4s.openapi.model.Schema.Property
import endpoints4s.openapi.model.Schema
import endpoints4s.Tupler

object DocumentedEndpoints
    extends FileUploadEndpoints
    with openapi.Endpoints
    with openapi.JsonEntities
    with openapi.ChunkedEntities {
  def authenticationToken[A](implicit
      request: Request[A]
  ): Response[AuthenticationToken] = ???

  protected def authenticatedRequest[U, E, H, UE, HCred, Out](
      method: Method,
      url: Url[U],
      entity: RequestEntity[E],
      headers: RequestHeaders[H],
      requestDocs: Option[String]
  )(implicit
      tuplerUE: Tupler.Aux[U, E, UE],
      tuplerHCred: Tupler.Aux[H, AuthenticationToken, HCred],
      tuplerUEHCred: Tupler.Aux[UE, HCred, Out]
  ): Request[Out] = DocumentedRequest(
    method,
    url,
    headers,
    requestDocs,
    Map()
  )

  protected def wheneverAuthenticated[A, B](response: Response[B])(implicit
      request: Request[A]
  ): Response[B] = List(
    DocumentedResponse(
      Unauthorized,
      "Jwt is not valid",
      DocumentedHeaders(List.empty),
      Map()
    ),
    DocumentedResponse(
      OK,
      "authentication successful",
      DocumentedHeaders(List.empty),
      Map()
    )
  )

  override def multipartFormRequestEntity: Map[String, MediaType] = Map(
    "multipart/form-data" -> MediaType(
      Some(
        endpoints4s
          .openapi
          .model
          .Schema
          .Object(
            List(
              Property(
                "file",
                Schema.Primitive(
                  "file",
                  Some("file"),
                  Some("The file to upload"),
                  None,
                  None
                ),
                true,
                None
              )
            ),
            None,
            None,
            None,
            Some("Files are uploaded using multipart/form-data")
          )
      )
    )
  )

  override def fileResponseEntity: Map[String, MediaType] = Map(
    "application/octet-stream" -> MediaType(
      Some(
        Primitive(
          "file",
          None,
          Some("The file to upload"),
          None,
          Some("The file")
        )
      )
    )
  )

  val api: OpenApi =
    openApi(Info(title = "API to upload files", version = "1.0"))(
      uploadEndpoint,
      downloadFileEndpoint,
      getFileMetadata,
      updateFileEndpoint
    )

}
