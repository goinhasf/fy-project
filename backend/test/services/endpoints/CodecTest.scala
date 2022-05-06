package services.endpoints

import org.scalatest.funspec.AnyFunSpec
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
import io.circe.generic.extras.Configuration
import shared.pages.PageContext
import shared.pages.RootPageContext
class CodecTest extends AnyFunSpec {
  it("should encode and decode the object with a discriminator") {
    implicit val genDevConfig =
      io.circe
        .generic
        .extras
        .Configuration
        .default
        .withDiscriminator("type")
    println((RootPageContext: PageContext).asJson.noSpaces)
    println(decode[PageContext](RootPageContext.asJson.noSpaces))
  }
}
