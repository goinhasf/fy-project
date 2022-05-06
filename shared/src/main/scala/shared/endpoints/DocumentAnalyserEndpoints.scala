package shared.endpoints

import endpoints4s.algebra
import io.circe.Json
import dao.forms.FormResourceFieldDescriptor

trait DocumentAnalyserEndpoints
    extends algebra.Endpoints
    with algebra.circe.JsonEntitiesFromCodecs {

  type MultipartForm
  type File
  val basePath = path / "api" / "doc"

  def fileResponseEntity: ResponseEntity[File]
  def multipartFormRequest: RequestEntity[MultipartForm]

  def analyseForm = endpoint(
    post(basePath / "inspect", multipartFormRequest),
    badRequest().orElse(ok(jsonResponse[Seq[FormResourceFieldDescriptor]]))
  )

  def fillForm = endpoint(
    post(basePath / "fill", multipartFormRequest),
    ok(fileResponseEntity)
  )

  def fillFormPdf = endpoint(
    post(basePath / "fill" /? qs[Boolean]("pdf"), multipartFormRequest),
    ok(fileResponseEntity)
  )

}
