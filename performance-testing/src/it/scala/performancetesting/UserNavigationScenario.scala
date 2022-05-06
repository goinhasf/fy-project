package performancetesting
import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._
import utils.RegisterUsersScenario
import shared.auth.UserCredentialsAuth
import io.circe.syntax._
import io.circe.parser._
import dao.events.EventWizardDescriptor
import dao.events.EventWizardState

object UserNavigationScenario {
  def apply() = {

    val headers_0 = Map(
      "Content-Type" -> "application/json",
      "Csrf-Token"   -> "nocheck",
      "Origin"       -> "http://localhost:8080"
    )

    val headers_1 = Map(
      "Accept"                    -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
      "Upgrade-Insecure-Requests" -> "1"
    )

    val headers_2 = Map(
      "Content-Type" -> "application/json",
      "Csrf-Token"   -> "nocheck",
      "Origin"       -> "http://localhost:8080"
    )

    val headers_3 = Map(
      "Content-type" -> "text/plain;charset=UTF-8",
      "Csrf-Token"   -> "nocheck",
      "Origin"       -> "http://localhost:8080"
    )

    val headers_5 = Map(
      "Content-Type" -> "application/json",
      "Csrf-Token"   -> "nocheck",
      "Origin"       -> "http://localhost:8080"
    )

    val headers_8 = Map("Accept" -> "image/webp,*/*")

    val headers_16 =
      Map("Csrf-Token" -> "nocheck", "Origin" -> "http://localhost:8080")

    val feeder = RegisterUsersScenario.createUsersFeeder(1000).toArray

    scenario("RecordedSimulation")
      .feed(feeder.circular)
      .exec(
        http("Get Login Page")
          .get("/login")
          .headers(headers_1)
      )
      .pause(2.seconds)
      .exec(
        http("Login")
          .post("/api/login")
          .headers(headers_0)
          .body(
            StringBody(
              UserCredentialsAuth("${username}", "password").asJson.noSpaces
            )
          )
          .check(
            header("Set-Cookie")
              .transform(_.stripPrefix("smSID="))
              .saveAs("cookie")
          )
      )
      .pause(1.seconds)
      .exec(addCookie(Cookie("smSID", "${cookie}")))
      .exec(
        http("Get Society Selection Page")
          .get("/select-society")
          .headers(headers_1)
          .check(
            header("Set-Cookie")
              .transform(_.stripPrefix("smSID="))
              .saveAs("cookie")
          )
      )
      .pause(1.seconds)
      .exec(addCookie(Cookie("smSID", "${cookie}")))
      .exec(
        http("Select Society")
          .post("/select-society")
          .headers(headers_3)
          .body(
            RawFileBody(
              "performancetesting/recordedsimulation/0003_request.txt"
            )
          )
          .check(
            header("Set-Cookie")
              .transform(_.stripPrefix("smSID="))
              .saveAs("cookie")
          )
      )
      .pause(2.seconds)
      .exec(addCookie(Cookie("smSID", "${cookie}")))
      .exec(
        http("Go to Events Page")
          .get("/events")
          .headers(headers_1)
      )
      .pause(2.seconds)
      .exec(addCookie(Cookie("smSID", "${cookie}")))
      .exec(
        http("Get social type event wizard")
          .get("/api/event-wizard/Social")
          .headers(headers_2)
          .check(
            bodyString
              .transform(
                decode[EventWizardDescriptor](_).fold(throw _, identity)
              )
              .transform(_._id)
              .saveAs("eventWizardId")
          )
          .resources(
            http("Create new draft")
              .post("/api/event-wizard/${eventWizardId}/new")
              .headers(headers_16)
              .check(bodyString.saveAs("eventDraftId"))
          )
      )
      .exec(addCookie(Cookie("smSID", "${cookie}")))
      .pause(2.seconds)
      .exec(
        http("Get Draft")
          .get(
            "/event-wizard/${eventWizardId}/state/${eventDraftId}"
          )
          .headers(headers_2)
      )
      .exec(addCookie(Cookie("smSID", "${cookie}")))
      .pause(2.seconds)
      .exec(
        http("See form resources")
          .get(
            "/form-resources"
          )
          .headers(headers_2)
      )
      .exec(addCookie(Cookie("smSID", "${cookie}")))
      .pause(2.seconds)
      .exec(
        http("See Tickets Form")
          .get(
            "/form-resources/60926f7b9dcf8f00debe3be8"
          )
          .headers(headers_2)
      )
  }
}
