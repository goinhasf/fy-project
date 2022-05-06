package shared.endpoints

import dao.forms.{FormCategory, FormResource, FormResourceDetails}
import endpoints4s.algebra
import dao.Ownership

import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.JsonObject
import dao.forms.FormResourceFieldDescriptor
import dao.forms.FormResourceFields
import io.circe.syntax._

trait FormResourceEndpoints
    extends algebra.Endpoints
    with algebra.circe.JsonEntitiesFromCodecs
    with MultipartFormEndpoints {
  val restPath = path / "api" / "form-resources"

  def getResource: Endpoint[String, Option[FormResource]] =
    endpoint(
      get(
        restPath / segment[String]("id")
      ),
      ok(jsonResponse[FormResource]).orNotFound()
    )

  def updateFormResource
      : Endpoint[(String, FormResourceDetails, String), Option[FormResource]] =
    endpoint(
      put(
        restPath / segment[String]("formId"),
        jsonRequest[FormResourceDetails],
        headers = requestHeader("Csrf-Token")
      ),
      ok(jsonResponse[FormResource]).orNotFound()
    )

  def getAllFormResources: Endpoint[Unit, Seq[FormResource]] =
    endpoint(
      get(
        restPath
      ),
      ok(jsonResponse[Seq[FormResource]])
    )

  def insertResource
      : Endpoint[(FormData, String), Either[ClientErrors, FormResource]] = {

    val response: Response[Either[ClientErrors, FormResource]] =
      badRequest(Some("Invalid form")).orElse(ok(jsonResponse[FormResource]))

    endpoint(
      post(
        restPath,
        multipartFormRequestEntity,
        headers = requestHeader("Csrf-Token")
      ),
      response
    )
  }
  def insertResourceWithExistingFile: Endpoint[
    (FormResourceDetails, String, String),
    FormResource
  ] = {

    endpoint(
      post(
        restPath / "full",
        jsonRequest[FormResourceDetails],
        headers = requestHeader("Csrf-Token") ++ requestHeader("Authorization")
      ),
      ok(jsonResponse[FormResource])
    )
  }

  implicit val jsonSegmentCodec: QueryStringParam[Json] =
    stringQueryString.xmap(_.asJson)(_.noSpaces)

  def formResourceAsPdf = endpoint(
    get(
      restPath / "pdf" / segment[String]("id") /? qs[Option[Json]](
        "obj"
      )
    ),
    ok(fileResponseEntity).orNotFound()
  )

  def getFormFields = endpoint(
    get(restPath / segment[String]("resourceId") / "fields"),
    ok(jsonResponse[FormResourceFields]).orNotFound()
  )

  def updateDefaultFieldValues = endpoint(
    put(
      restPath / segment[String]("formResourceId") / "update-values",
      jsonRequest[JsonObject],
      headers = requestHeader("Csrf-Token")
    ),
    ok(jsonResponse[FormResource]).orNotFound()
  )

}
