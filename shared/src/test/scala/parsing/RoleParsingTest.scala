package parsing

import org.scalatest.funspec.AnyFunSpec

import io.circe.parser._
import io.circe.syntax._
import shared.auth.Role

class RoleParsingTest extends AnyFunSpec {

  import io.circe.generic.extras.auto._
  import io.circe.generic.extras.Configuration

  implicit val genDevConfig: Configuration = Configuration
    .default
    .withDiscriminator("_t")

  it("should encode the role type correctly") {
    println(Role.Admin().roleType.asJson)
  }
}
