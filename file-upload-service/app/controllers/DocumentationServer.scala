package controllers

import com.google.inject._
import endpoints4s.openapi.model.OpenApi
import endpoints4s.play.server
import endpoints4s.play.server.PlayComponents
import play.api.http.DefaultHttpErrorHandler
import play.api.mvc.ControllerComponents
import play.api.routing.Router
import play.api.routing.SimpleRouter

import controllers.PlayComponentsController
import endpoints4s.Valid
import endpoints4s.Invalid

@Singleton
class DocumentationServer @Inject() (
    cc: ControllerComponents,
    errorHandler: DefaultHttpErrorHandler
) extends PlayComponentsController(cc, errorHandler)
    with SimpleRouter
    with server.Endpoints
    with server.JsonEntitiesFromEncodersAndDecoders
    with server.Assets {

  override def assetSegments(
      name: String,
      docs: endpoints4s.algebra.Documentation
  ): Path[AssetPath] = {
    val stringPath = segment[String](name, docs)
    new Path[AssetPath] {
      def decode(segments: List[String]) =
        segments.reverse match {
          case s :: p =>
            val i = s.lastIndexOf('-')
            if (i > 0) {
              val (name, digest) = s.splitAt(i)
              Some((Valid(AssetPath(p.reverse, digest.drop(1), name)), Nil))
            } else Some((Invalid("Invalid asset segments"), Nil))
          case Nil => None
        }
      def encode(s: AssetPath) =
        s.path
          .foldRight(stringPath.encode(s"${s.digest}-${s.name}"))(
            (segment, path) => s"${stringPath.encode(segment)}/$path"
          )
    }

  }

  override def digests: Map[String, String] = Map(
    "index.html" -> "3c09b175732f72a887bb60cafc9571cb"
  )

  // HTTP endpoint serving documentation. Uses the HTTP verb ''GET'' and the path
  // ''/documentation.json''. Returns an OpenAPI document.
  val docs = endpoint[Unit, OpenApi](
    get(path / "documentation.json"),
    ok(jsonResponse[OpenApi])
  )

  // We “render” the OpenAPI document using the swagger-ui, provided as static assets
  val assets = assetsEndpoint(path / "assets" / assetSegments())

  // Redirect the root URL “/” to the “index.html” asset for convenience
  val root = endpoint(get(path), redirect(assets)(asset("index.html")))

  val routes = routesFromEndpoints(
    docs.implementedBy(_ => DocumentedEndpoints.api),
    assets.implementedBy(assetsResources(pathPrefix = Some("/public"))),
    root
  )

}
