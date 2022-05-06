package shared.endpoints

import endpoints4s.algebra

trait HtmlEndpoints extends algebra.Endpoints {
  type HtmlPage
  def pageResponse: ResponseEntity[HtmlPage]
}
