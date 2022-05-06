package views

import play.api.http.Writeable
import akka.util.ByteString

trait AppTemplate {
  def render(): String
}
object AppTemplate {
  implicit val writeableView: Writeable[AppTemplate] = new Writeable[AppTemplate](
    view => ByteString(view.render()),
    Some("text/html")
  )
}
