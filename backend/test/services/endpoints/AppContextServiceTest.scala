package services.endpoints

import org.scalatest.funspec.AnyFunSpec
import org.scalamock.scalatest.MockFactory
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.bind
import shared.config.MicroServiceConfig
import play.api.libs.ws.WSClient
import play.api.Mode
import play.api.test.WsTestClient
import io.circe._, io.circe.parser._, io.circe.syntax._
import io.circe.generic.semiauto._
import shared.pages.PageContext, PageContext._
import shared.pages.RootPageContext
import shared.pages.content.ContextContent
import shared.pages.content.RootPageContent
import play.core.server.Server
import scala.concurrent.Await
import play.core.server.ServerConfig
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import endpoints4s.play.client
import shared.endpoints.context.PageContextEndpoints
import play.api.mvc.ControllerComponents
import endpoints4s.Validated
import endpoints4s.Valid
import endpoints4s.Invalid
import shared.config.JwtConfig
import utils.MicroServiceConfigLoader
import scala.concurrent.Future
import org.scalatest.compatible.Assertion
import shared.pages.NoContext
import shared.pages.content.EmptyContent
import play.api.inject
import endpoints4s.algebra.MuxRequest
import play.api.libs.ws.WSResponse
import endpoints4s.Tupler
import play.api.libs.ws.BodyWritable
import play.api.http.ContentTypes
import play.api.libs.ws.WSBody
import akka.util.ByteString
import play.api.libs.ws.InMemoryBody
import scala.concurrent.ExecutionContext
import io.circe.generic.extras.Configuration
import clients.play.auth.AuthorizationClient
import shared.auth.ApiKey

class PageContextServiceTest extends AnyFunSpec with MockFactory {

  describe("Page context service tests") {

    val port = 9100

    val app = new GuiceApplicationBuilder()
      .overrides(
        bind[MicroServiceConfig].toInstance(new MicroServiceConfig {
          override val serviceUrl: String   = s"http://localhost:$port"
          override val jwtConfig: JwtConfig = mock[JwtConfig]
          override val apiKey: ApiKey       = ApiKey("test", "abcd")
        })
      )
      .in(Mode.Test)

    def pageContextClientFactory(injector: inject.Injector) =
      PageContextClient(
        injector.instanceOf[MicroServiceConfig],
        injector.instanceOf[WSClient],
        injector.instanceOf[ControllerComponents]
      )

    def testPageContextClient(
        test: PageContextClient => Future[Assertion]
    ): Assertion = {
      val appInstance = app.build()
      Server.withApplication(
        appInstance,
        config = ServerConfig(port = Some(port))
      )(port =>
        Await.result(
          test(pageContextClientFactory(appInstance.injector)),
          Duration.Inf
        )
      )
    }

    it("should return a root content when asking for a root context") {

      testPageContextClient {
        _.getPageContext((RootPageContext, "token"))
          .map { content =>
            assert(content == RootPageContent("Hello", ???))
          } recoverWith { case t: Throwable =>
          t.printStackTrace()
          fail("Error getting context", t)
        }
      }

    }

    it("should return an empty content when asking for a Empty context") {

      testPageContextClient {
        _.getPageContext((NoContext, "token"))
          .map { content =>
            assert(content == EmptyContent())
          } recoverWith { case t: Throwable =>
          t.printStackTrace()
          fail("Error getting context", t)
        }
      }

    }
  }

}

class MuxEndpointsWithHeadersPlayClient(
    serviceUrl: String,
    ws: WSClient
)(implicit ec: ExecutionContext)
    extends client.Endpoints(serviceUrl, ws)(ec)
    with shared.endpoints.MuxEndpointWithHeaders[Json]
    with client.MuxEndpoints
    with client.JsonEntitiesFromCodecs {

  implicit val genDevConfig: Configuration =
    Configuration.default.withDiscriminator("__type")

  class MuxRequestWH[U, Req <: MuxRequest, ReqHeaders, UReq, UReqH](
      url: Url[U],
      entity: RequestEntity[Req],
      headers: RequestHeaders[ReqHeaders]
  )(implicit
      codec: JsonCodec[Req],
      tuplerUReq: Tupler.Aux[U, Req, UReq],
      tuplerUReqH: Tupler.Aux[UReq, ReqHeaders, UReqH]
  ) {
    def toRequest = request[U, Req, ReqHeaders, UReq, UReqH](
      Post,
      url,
      (req, wsRequest) => {
        implicit val writeable = BodyWritable[Req](
          entity =>
            InMemoryBody(ByteString(codec.encoder(entity).noSpacesSortKeys)),
          ContentTypes.JSON
        )
        entity(req, wsRequest).withBody(req)
      },
      headers = headers
    )
  }

  class MuxResponse[Res](
      statusCode: StatusCode,
      entity: ResponseEntity[Res]
  )(implicit
      codec: JsonCodec[Res]
  ) {
    def toResponse: Response[Res] =
      response(
        statusCode,
        res => {
          decode[Res](res.body)(codec.decoder)
        }
      )
  }

  class MuxEndpointWithHeaders[
      U,
      Req <: MuxRequest,
      ReqHeaders,
      Res,
      UReq,
      UReqH
  ](
      muxRequest: MuxRequestWH[U, Req, ReqHeaders, UReq, UReqH],
      muxResponse: MuxResponse[Res]
  )(implicit
      codec: JsonCodec[Req],
      tuplerUReq: Tupler.Aux[U, Req, UReq],
      tuplerUReqH: Tupler.Aux[UReq, ReqHeaders, UReqH]
  ) {
    def apply(entity: UReqH) =
      endpoint(muxRequest.toRequest, muxResponse.toResponse).apply(entity)
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
      status: StatusCode,
      res: ResponseEntity[Res]
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

case class PageContextClient(
    microServiceConfig: MicroServiceConfig,
    ws: WSClient,
    cc: ControllerComponents
) extends MuxEndpointsWithHeadersPlayClient(microServiceConfig.serviceUrl, ws)(
      cc.executionContext
    )
    with PageContextEndpoints
