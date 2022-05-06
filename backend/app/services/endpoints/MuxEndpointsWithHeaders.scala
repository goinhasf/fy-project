package services.endpoints

import endpoints4s.play.server
import endpoints4s.algebra.MuxRequest
import endpoints4s.{Decoder, Encoder}
import play.api.mvc.Call
import endpoints4s.Tupler
import play.api.http.HttpEntity
import play.api.mvc.Results
import scala.concurrent.Future
import endpoints4s.play.server.MuxHandler
import endpoints4s.play.server.MuxHandlerAsync
import scala.util.control.NonFatal
import play.api.libs.streams.Accumulator
import play.api.mvc.EssentialAction
import endpoints4s.Valid
import endpoints4s.Invalid
import play.api.http.Status
import scala.concurrent.ExecutionContext
import io.circe.Json
import io.circe.DecodingFailure
import play.api.mvc.RequestHeader
import clients.play.auth.AuthorizationClient
import play.api.mvc.Handler
import play.api.mvc.Session
import play.api.mvc.Result
import services.endpoints.handlers.{
  ActionHandlers,
  JwtValidator,
  SessionHandler
}
import shared.config.JwtConfig
import dao.users.UserInfo

trait MuxEndpointsWithHeaders
    extends shared.endpoints.MuxEndpointWithHeaders[Json]
    with server.MuxEndpoints
    with server.Endpoints
    with ActionHandlers
    with SessionHandler
    with JwtValidator {

  trait MuxHandlerAsyncHandler1[Req <: MuxRequest, Resp, A] {
    def apply[R <: Resp](req: Req { type Response = R }, a: A): Future[R]
  }

  trait MuxHandlerAsyncHandler2[Req <: MuxRequest, Resp, A, B] {
    def apply[R <: Resp](req: Req { type Response = R }, a: A, b: B): Future[R]
  }

  class MuxRequestWH[U, Req, ReqHeaders, UReq, UReqH](
      val url: Url[U],
      val reqEntity: RequestEntity[Req],
      val headers: RequestHeaders[ReqHeaders]
  )(implicit
      codec: JsonCodec[Req],
      tuplerUReq: Tupler.Aux[U, Req, UReq],
      tuplerUReqH: Tupler.Aux[UReq, ReqHeaders, UReqH]
  ) {
    def toRequest(implicit tupler: Tupler.Aux[U, Json, UReq]) = {

      val reqEntityMapped = reqEntity.xmap(codec.encoder.apply)(
        codec
          .decoder
          .decodeJson(_)
          .fold(
            errors => throw new Exception(errors),
            identity
          )
      )
      request(
        Post,
        url,
        reqEntityMapped,
        headers = headers
      )
    }
  }

  class MuxResponse[Res](
      status: StatusCode,
      res: ResponseEntity[Res]
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
      codec: JsonCodec[Req],
      tuplerUReq: Tupler.Aux[U, Req, UReq],
      tuplerUReqH: Tupler.Aux[UReq, ReqHeaders, UReqH]
  ) {

    implicit val tuplerUT = new Tupler[U, Json] {
      type Out = UReq
      def apply(a: U, b: Json): UReq = tuplerUReq(
        a,
        codec
          .decoder
          .decodeJson(b)
          .fold(
            f => throw new Exception("Error decoding json value", f),
            identity
          )
      )
      def unapply(out: UReq): (U, Json) = {
        val (u, req) = tuplerUReq.unapply(out)
        (u, codec.encoder(req))
      }
    }

    protected def playHandler[R <: Res](
        endpoint: Endpoint[UReqH, Res],
        jwtHandler: JwtHandler,
        sessionHandler: SessionContentExtractor,
        muxAsyncHandler: MuxHandlerAsyncHandler2[
          Req,
          Res,
          UserInfo,
          SessionInfo,
        ]
    )(implicit ec: ExecutionContext) =
      EndpointWithHandler2[UReqH, Res, UserInfo, SessionInfo](
        endpoint,
        jwtHandler,
        sessionHandler,
        (ureqh, a, b) => {
          val (ureq, h) = tuplerUReqH.unapply(ureqh)
          val (u, req)  = tuplerUReq.unapply(ureq)
          muxAsyncHandler.apply(
            req.asInstanceOf[Req { type Response = R }],
            a,
            b
          )
        }
      )

    def implementedByAsync(
        muxAsyncHandler: MuxHandlerAsyncHandler2[
          Req,
          Res,
          UserInfo,
          SessionInfo
        ]
    )(implicit ec: ExecutionContext, jwtConfig: JwtConfig) = {
      playHandler(
        endpoint(request.toRequest, response.toResponse),
        sessionOrAuthorizationValidator(Results.Unauthorized("Jwt Missing")),
        sessionExtractor,
        muxAsyncHandler
      )
    }
  }

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
      status: Results.Status,
      res: ResponseEntity[Res]
  )(implicit
      codec: JsonCodec[Res]
  ): MuxResponse[Res] =
    new MuxResponse(status, res)

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
