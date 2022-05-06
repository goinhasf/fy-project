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

object Protocol {

  def apply() = http
    .baseUrl("http://localhost:8080")
    .inferHtmlResources(
      BlackList(
        """.*\.js""",
        """.*\.css""",
        """.*\.gif""",
        """.*\.jpeg""",
        """.*\.jpg""",
        """.*\.ico""",
        """.*\.woff""",
        """.*\.woff2""",
        """.*\.(t|o)tf""",
        """.*\.png""",
        """.*detectportal\.firefox\.com.*"""
      ),
      WhiteList()
    )
    .acceptHeader("*/*")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-GB,en;q=0.5")
    .doNotTrackHeader("1")
    .userAgentHeader(
      "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:87.0) Gecko/20100101 Firefox/87.0"
    )
}

class LoadTestingOnNavigationToEventsSimulation extends Simulation {

  setUp(
    UserNavigationScenario().inject(
      rampConcurrentUsers(0).to(300).during(60.seconds)
    )
  ).protocols(Protocol())
}

class StressTestingOnNavigationToEventsSimulation extends Simulation {

  setUp(
    UserNavigationScenario().inject(
      atOnceUsers(500)
    )
  ).protocols(Protocol())
}

class SpikeTestingOnNavigationToEventsSimulation extends Simulation {

  setUp(
    UserNavigationScenario().inject(
      rampUsers(100).during(5.seconds),
      rampUsers(200).during(5.seconds),
      rampUsers(300).during(5.seconds)
    )
  ).protocols(Protocol())
}

class EnduranceTestingOnNavigationToEventsSimulation extends Simulation {

  setUp(
    UserNavigationScenario().inject(
      constantConcurrentUsers(250).during(3.minutes)
    )
  ).protocols(Protocol())
}
