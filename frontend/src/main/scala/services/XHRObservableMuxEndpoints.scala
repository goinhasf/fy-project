package services

import endpoints4s.algebra.MuxRequest
import scala.concurrent.Promise
import endpoints4s.Encoder
import endpoints4s.Decoder
import shared.endpoints.MuxEndpointWithHeaders
import endpoints4s.Tupler
import org.scalajs.dom.raw.XMLHttpRequest
import com.raquo.laminar.api.L.EventStream
import endpoints4s.xhr

import scalajs.js
import io.circe.Json

trait XHRObservableMuxEndpoints
    extends XHRObservableEndpoints
    with xhr.MuxEndpoints
    with MuxEndpointWithHeaders[Json] {

  class MuxEndpoint[Req <: MuxRequest, Resp, Transport](
      request: Request[Transport],
      response: Response[Transport]
  ) {
    def apply(entity: Req)(implicit
        encoder: Encoder[Req, Transport],
        decoder: Decoder[Transport, Any]
    ): EventStream[entity.Response] = {
      val promise = Promise[entity.Response]()
      muxPerformXhr(request, response, entity)(
        _.fold(
          exn => { promise.failure(exn); () },
          b => {
            promise.success(b); ()
          }
        ),
        xhr => { promise.failure(new Exception(xhr.responseText)); () }
      )
      EventStream.fromFuture(promise.future)
    }
  }

  class MuxRequestWH[U, Req, ReqHeaders, UReq, UReqH](
      url: Url[U],
      entity: RequestEntity[Req],
      headers: RequestHeaders[ReqHeaders]
  )(implicit
      codec: JsonCodec[Req],
      tuplerUReq: Tupler.Aux[U, Req, UReq],
      tuplerUReqH: Tupler.Aux[UReq, ReqHeaders, UReqH]
  ) {
    def toRequest = request(Post, url, entity, headers = headers)
  }

  class MuxResponse[Res](
      status: Int,
      res: js.Function1[XMLHttpRequest, Either[Throwable, Res]]
  )(implicit
      codec: JsonCodec[Res]
  ) {
    def toResponse = response(status, res)
  }

  class MuxEndpointWithHeaders[
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
      tuplerUReqH: Tupler.Aux[UReq, ReqHeaders, UReqH]
  ) {
    def apply(req: UReqH): EventStream[Res] =
      endpoint(request.toRequest, response.toResponse).apply(req)
  }

  def muxEndpoint[Req <: MuxRequest, Resp, Transport](
      request: Request[Transport],
      response: Response[Transport]
  ): MuxEndpoint[Req, Resp, Transport] = new MuxEndpoint(request, response)

  def muxRequest[U, Req <: MuxRequest, ReqHeaders, UReq, UReqH](
      url: Url[U],
      entity: RequestEntity[Req],
      headers: RequestHeaders[ReqHeaders]
  )(implicit
      codec: JsonCodec[Req],
      tuplerUReq: Tupler.Aux[U, Req, UReq],
      tuplerUReqH: Tupler.Aux[UReq, ReqHeaders, UReqH]
  ): MuxRequestWH[U, Req, ReqHeaders, UReq, UReqH] =
    new MuxRequestWH(url, entity, headers)

  def muxResponse[Res](
      status: Int,
      res: js.Function1[XMLHttpRequest, Either[Throwable, Res]]
  )(implicit
      codec: JsonCodec[Res]
  ): MuxResponse[Res] = new MuxResponse(status, res)

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
  ): MuxEndpointWithHeaders[U, Req, ReqHeaders, Res, UReq, UReqH] =
    new MuxEndpointWithHeaders(request, response)

}
