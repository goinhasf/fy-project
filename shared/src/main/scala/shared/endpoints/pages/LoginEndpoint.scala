package shared.endpoints.pages

import shared.endpoints.HtmlEndpoints

trait LoginEndpoint { self: HtmlEndpoints =>
  def loginPage        = endpoint(get(path / "login"), ok(pageResponse))
  def unauthorizedPage = endpoint(get(path / "unauthorized"), ok(pageResponse))
}
