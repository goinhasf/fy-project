package services.util

import org.scalajs.dom
import services.ClientService

trait CsrfTokenConsumer { self: ClientService => 
  def getCsrfToken(): Option[String]
}
trait DefaultCsrfTokenConsumer extends CsrfTokenConsumer { self: ClientService =>
  def getCsrfToken(): Option[String] = Option(
    dom
      .document
      .getElementById("csrf")
      .getAttribute("token-value")
  )
}
