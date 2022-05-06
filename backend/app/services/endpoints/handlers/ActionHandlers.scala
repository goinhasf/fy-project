package services.endpoints.handlers

import play.api.mvc.Handler
import play.api.mvc.RequestHeader
import endpoints4s.play.server
import play.api.mvc.EssentialAction
import play.api.mvc.Session
import play.api.mvc.Results
import scala.concurrent.Future
import play.api.mvc.Result
import play.api.http.Status
import scala.util.Try
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal
import play.api.mvc.Action
import play.api.libs.streams.Accumulator
import akka.util.ByteString
import play.api.mvc.AnyContent
trait ActionHandlers extends server.Endpoints {

  type RequestHandler[A]      = RequestHeader => Either[Result, A]
  type AsyncRequestHandler[A] = RequestHeader => Future[Either[Result, A]]

  case class ResponseObject[A, B](
      requestArgs: A,
      responseBody: B,
      result: Result
  )

  type AsyncResponseHandler[A, B] =
    (ResponseObject[A, B], RequestHeader) => Future[Result]
  implicit def toAsyncActionHandler[A](
      handler: RequestHandler[A]
  ): AsyncRequestHandler[A] = header => Future.successful(handler(header))

  private def bodyParserErrorResult(implicit headers: RequestHeader) =
    playComponents
      .httpErrorHandler
      .onClientError(headers, Status.UNSUPPORTED_MEDIA_TYPE)

  private def applyToAction(result: Result): Action[AnyContent] = playComponents
    .defaultActionBuilder(result)

  private def applyFutureToAction(future: Future[Result]): Action[AnyContent] =
    playComponents
      .defaultActionBuilder
      .async(future)

  private def futureToAccumulator(
      future: Future[Result]
  )(implicit ec: ExecutionContext, requestHeader: RequestHeader) = {
    val action = applyFutureToAction(future)
    action(requestHeader).recover { case NonFatal(t) =>
      handleServerError(t)
    }
  }

  private def resultToAccumulator(result: Result)(implicit
      ec: ExecutionContext,
      requestHeader: RequestHeader
  ) = applyToAction(result).apply(requestHeader).recover { case NonFatal(t) =>
    handleServerError(t)
  }

  private def handleActionError(
      action: Action[Result]
  )(implicit
      ec: ExecutionContext,
      headers: RequestHeader
  ): Accumulator[ByteString, Result] =
    action(headers).recover { case NonFatal(t) =>
      handleServerError(t)
    }

  def asyncHandler1[A, B, C](
      header: RequestHeader,
      endpoint: Endpoint[A, B],
      requestHandler: AsyncRequestHandler[C],
      bodyAction: (A, C) => Future[B],
      responseHandler: AsyncResponseHandler[(A, C), B]
  )(implicit ec: ExecutionContext) = {
    try {
      endpoint
        .request
        .decode(header)
        .map { requestEntity =>
          EssentialAction { implicit headers =>
            try {
              requestEntity(headers) match {
                case Some(bodyParser) =>
                  val action = playComponents
                    .defaultActionBuilder
                    .async(bodyParser) { request =>
                      val future = for {
                        h <- requestHandler(headers)
                      } yield h match {
                        case Left(value) => Future.successful(value)
                        case Right(c) =>
                          bodyAction(request.body, c)
                            .map { b =>
                              ResponseObject(
                                (request.body, c),
                                b,
                                endpoint.response(b)
                              )
                            }
                            .flatMap(responseObject => responseHandler(responseObject, headers))
                      }
                      future.flatten
                    }
                  action(headers)
                // Unable to handle request entity
                case None =>
                  Accumulator.done(
                    playComponents
                      .httpErrorHandler
                      .onClientError(headers, Status.UNSUPPORTED_MEDIA_TYPE)
                  )
              }
            } catch {
              case NonFatal(t) => Accumulator.done(handleServerError(t))
            }
          }
        }
    } catch {
      case NonFatal(t) =>
        Some(playComponents.defaultActionBuilder(_ => handleServerError(t)))
    }
  }

  def asyncHandler2[A, B, C, D](
      header: RequestHeader,
      endpoint: Endpoint[A, B],
      actionHandler1: AsyncRequestHandler[C],
      actionHandler2: AsyncRequestHandler[D],
      bodyAction: (A, C, D) => Future[B],
      sessionHandler: RequestHeader => Session = req => req.session
  )(implicit ec: ExecutionContext) = {
    try {
      endpoint
        .request
        .decode(header)
        .map { requestEntity =>
          EssentialAction { implicit headers =>
            try {
              requestEntity(headers) match {
                case Some(bodyParser) =>
                  val action = playComponents
                    .defaultActionBuilder
                    .async(bodyParser) { request =>
                      val future = for {
                        fc <- actionHandler1(headers)
                        fd <- actionHandler2(headers)
                      } yield {
                        val eitherResult = for {
                          c <- fc
                          d <- fd
                        } yield bodyAction(request.body, c, d)
                          .map(endpoint.response)
                          .map(_.withSession(sessionHandler(request)))

                        eitherResult match {
                          case Left(value)  => Future.successful(value)
                          case Right(value) => value
                        }
                      }

                      future.flatten
                    }
                  action(headers)
                // Unable to handle request entity
                case None =>
                  Accumulator.done(
                    playComponents
                      .httpErrorHandler
                      .onClientError(headers, Status.UNSUPPORTED_MEDIA_TYPE)
                  )
              }
            } catch {
              case NonFatal(t) => Accumulator.done(handleServerError(t))
            }
          }
        }
    } catch {
      case NonFatal(t) =>
        Some(playComponents.defaultActionBuilder(_ => handleServerError(t)))
    }
  }

  def asyncHandler3[A, B, C, D, E](
      header: RequestHeader,
      endpoint: Endpoint[A, B],
      actionHandler1: AsyncRequestHandler[C],
      actionHandler2: AsyncRequestHandler[D],
      actionHandler3: AsyncRequestHandler[E],
      bodyAction: (A, C, D, E) => Future[B],
      sessionHandler: RequestHeader => Session = req => req.session
  )(implicit ec: ExecutionContext) = {
    try {
      endpoint
        .request
        .decode(header)
        .map { requestEntity =>
          EssentialAction { implicit headers =>
            try {
              requestEntity(headers) match {
                case Some(bodyParser) =>
                  val action = playComponents
                    .defaultActionBuilder
                    .async(bodyParser) { request =>
                      val future = for {
                        fc <- actionHandler1(headers)
                        fd <- actionHandler2(headers)
                        fe <- actionHandler3(headers)
                      } yield {
                        val eitherResult = for {
                          c <- fc
                          d <- fd
                          e <- fe
                        } yield bodyAction(request.body, c, d, e)
                          .map(endpoint.response)
                          .map(_.withSession(sessionHandler(request)))

                        eitherResult match {
                          case Left(value)  => Future.successful(value)
                          case Right(value) => value
                        }
                      }

                      future.flatten
                    }
                  action(headers)
                // Unable to handle request entity
                case None =>
                  Accumulator.done(
                    playComponents
                      .httpErrorHandler
                      .onClientError(headers, Status.UNSUPPORTED_MEDIA_TYPE)
                  )
              }
            } catch {
              case NonFatal(t) => Accumulator.done(handleServerError(t))
            }
          }
        }
    } catch {
      case NonFatal(t) =>
        Some(playComponents.defaultActionBuilder(_ => handleServerError(t)))
    }
  }

  case class EndpointWithHandler1[A, B, C](
      endpoint: Endpoint[A, B],
      actionHandler: AsyncRequestHandler[C],
      bodyAction: (A, C) => Future[B],
      responseHandler: AsyncResponseHandler[(A, C), B] =
        (r: ResponseObject[(A, C), B], _: RequestHeader) => Future.successful(r.result)
  )(implicit ec: ExecutionContext)
      extends ToPlayHandler {
    def playHandler(header: RequestHeader): Option[Handler] = asyncHandler1(
      header,
      endpoint,
      actionHandler,
      bodyAction,
      responseHandler
    )
  }
  case class EndpointWithHandler2[A, B, C, D](
      endpoint: Endpoint[A, B],
      actionHandler1: AsyncRequestHandler[C],
      actionHandler2: AsyncRequestHandler[D],
      bodyAction: (A, C, D) => Future[B]
  )(implicit ec: ExecutionContext)
      extends ToPlayHandler {
    def playHandler(header: RequestHeader): Option[Handler] = asyncHandler2(
      header,
      endpoint,
      actionHandler1,
      actionHandler2,
      bodyAction
    )
  }
  case class EndpointWithHandler3[A, B, C, D, E](
      endpoint: Endpoint[A, B],
      actionHandler1: AsyncRequestHandler[C],
      actionHandler2: AsyncRequestHandler[D],
      actionHandler3: AsyncRequestHandler[E],
      bodyAction: (A, C, D, E) => Future[B]
  )(implicit ec: ExecutionContext)
      extends ToPlayHandler {
    def playHandler(header: RequestHeader): Option[Handler] = asyncHandler3(
      header,
      endpoint,
      actionHandler1,
      actionHandler2,
      actionHandler3,
      bodyAction
    )
  }

  def actionHandlerUnion[A, B, C](
      aHandler: AsyncRequestHandler[A],
      bHandler: AsyncRequestHandler[B]
  )(f: (A, B) => C)(implicit ec: ExecutionContext): AsyncRequestHandler[C] = {
    req =>
      for {
        aRes <- aHandler(req)
        bRes <- bHandler(req)
      } yield {
        for {
          a <- aRes
          b <- bRes
        } yield f(a, b)
      }
  }

}
