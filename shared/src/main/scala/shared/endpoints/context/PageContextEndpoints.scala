package shared.endpoints.context

import endpoints4s.Invalid
import endpoints4s.Tupler
import endpoints4s.Valid
import endpoints4s.Validated
import endpoints4s.algebra
import endpoints4s.algebra.MuxRequest
import endpoints4s.circe.JsonSchemas
import io.circe.Json
import io.circe.generic
import shared.endpoints.MuxEndpointWithHeaders
import shared.endpoints.authentication.Authentication
import shared.pages.PageContext
import shared.pages.content.ContextContent
import endpoints4s.Encoder
import endpoints4s.Decoder
import endpoints4s.Codec
import endpoints4s.algebra.circe.CirceCodec
import io.circe

trait PageContextEndpoints
    extends MuxEndpointWithHeaders[Json]
    with algebra.circe.JsonEntitiesFromCodecs {

  def getPageContext[Res <: ContextContent] = {
    muxEndpointWHeaders(
      muxRequest(
        path / "api" / "context",
        jsonRequest[PageContext],
        requestHeader("Csrf-Token")
      ),
      muxResponse(OK, jsonResponse[ContextContent])
    )
  }
}
