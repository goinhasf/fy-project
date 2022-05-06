package services

import com.raquo.laminar.api.L.EventStream
import endpoints4s.xhr

import scalajs.js

trait EventStreamWithCustomErrors extends xhr.EndpointsWithCustomErrors {

  type Result[A] = EventStream[A]

  def endpoint[A, B](
      request: Request[A],
      response: Response[B],
      docs: EndpointDocs = EndpointDocs()
  ): Endpoint[A, B] =
    new Endpoint[A, B](request) {
      def apply(a: A) = {
        EventStream.fromJsPromise(new js.Promise[B]((resolve, error) => {
          performXhr(request, response, a)(
            _.fold(exn => error(exn.getMessage), b => resolve(b)): Unit,
            xhr => error(xhr.responseText): Unit
          )
        }))
      }
    }
}

trait XHRObservableEndpoints
    extends xhr.Endpoints
    with EventStreamWithCustomErrors {}
