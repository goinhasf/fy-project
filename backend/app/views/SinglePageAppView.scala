package views
import akka.util.ByteString
import play.api.http.Writeable
import scalatags.Text.all.{html => Html, _}
import scalatags.Text.tags2.{title => T}
import scalajs.html._
import play.api.Logger
import play.filters.csrf.CSRF.Token
import controllers.routes
import java.io.File
import scalatags.Text.TypedTag
import scala.util.Try
import play.api.mvc.RequestHeader
import play.filters.csrf.CSRF
import play.api.mvc.Request
import play.api.mvc.Result
import play.api.mvc.Results

object SinglePageAppView {
  def getSinglePageAppView(
      titleString: String,
      csrfToken: Token
  ): AppTemplate = new AppTemplate {
    val logger = Logger(classOf[AppTemplate])

    val styleSheets = Seq(
      "main.css",
      "card.css",
      "textfield.css",
      "forms.css",
    )

    def render(): String = {
      val styles = getAllStyles().fold[Seq[TypedTag[String]]](
        t => {
          logger.error("Failed to get styles", t)
          Seq()
        },
        seq => seq
      )

      val resourceExists =
        (asset: String) => getClass.getResource(s"/public/$name") != null

      val output = "<!DOCTYPE html>" + Html(
        head(
          T(titleString),
          meta(charset := "UTF-8"),
          meta(
            content := "initial-sale=1, width=device-width",
            name := "viewport"
          ),
          link(
            href := "https://unpkg.com/material-components-web@latest/dist/material-components-web.min.css",
            rel := "stylesheet"
          ),
          link(
            rel := "stylesheet",
            href := "https://fonts.googleapis.com/icon?family=Material+Icons"
          ),
          link(
            rel := "preconnect",
            href := "https://fonts.gstatic.com"
          ),
          link(
            rel := "stylesheet",
            href := "https://fonts.googleapis.com/css2?family=Roboto&display=swap"
          ),
          styles
        ),
        body(
          div(
            id := "csrf",
            attr("token-value") := csrfToken.value
          ),
          div(id := "app-root"),
          script(
            src := controllers
              .routes
              .Assets
              .versioned(s"frontend-fastopt-library.js")
              .url
          ),
          raw(
            """
          |<script language="JavaScript">
          |var exports = window;
          |exports.require = window["ScalaJSBundlerLibrary"].require;
          |</script>
          """.stripMargin
          ),
          script(
            src := controllers
              .routes
              .Assets
              .versioned(s"frontend-fastopt.js")
              .url
          )
        )
      ).render
      logger.info(output)
      output

    }

    def getAllStyles(): Try[Seq[TypedTag[String]]] = {
      Try {
        styleSheets
          .map(f =>
            link(
              rel := "stylesheet",
              href := routes.Assets.versioned(s"stylesheets/${f}").url
            )
          )
      }
    }
  }

}
