package shared.endpoints

import endpoints4s.algebra
import endpoints4s.algebra.MuxRequest
import endpoints4s.Tupler
import endpoints4s.Encoder
import endpoints4s.Decoder
import endpoints4s.Codec

trait MuxEndpointWithHeaders[Transport]
    extends algebra.MuxEndpoints
    with algebra.circe.JsonEntitiesFromCodecs
    with algebra.Endpoints
    with algebra.BuiltInErrors {

  type MuxRequestWH[U, Req <: MuxRequest, ReqHeaders, UReq, UReqH]
  type MuxResponse[Res]
  type MuxEndpointWithHeaders[
      U,
      Req <: MuxRequest,
      ReqHeaders,
      Res,
      UReq,
      UReqH
  ]

  def muxRequest[U, Req <: MuxRequest, ReqHeaders, UReq, UReqH](
      url: Url[U],
      entity: RequestEntity[Req],
      headers: RequestHeaders[ReqHeaders]
  )(implicit
      codec: JsonCodec[Req],
      tuplerUReq: Tupler.Aux[U, Req, UReq],
      tuplerUReqH: Tupler.Aux[UReq, ReqHeaders, UReqH]
  ): MuxRequestWH[U, Req, ReqHeaders, UReq, UReqH]

  def muxResponse[Res](
      status: StatusCode,
      res: ResponseEntity[Res]
  )(implicit
      codec: JsonCodec[Res]
  ): MuxResponse[Res]

  def muxEndpointWHeaders[
      U,
      Req <: MuxRequest,
      ReqHeaders,
      Res,
      UReq,
      UReqH
  ](
      request: MuxRequestWH[U, Req, ReqHeaders, UReq, UReqH],
      response: MuxResponse[Res]
  )(implicit
      tuplerUReq: Tupler.Aux[U, Req, UReq],
      tuplerUReqH: Tupler.Aux[UReq, ReqHeaders, UReqH],
      codecReq: JsonCodec[Req],
      codecRes: JsonCodec[Res]
  ): MuxEndpointWithHeaders[U, Req, ReqHeaders, Res, UReq, UReqH]

}
