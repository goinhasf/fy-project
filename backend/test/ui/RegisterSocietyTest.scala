package ui

import ui.utils.UISpec
import org.scalatestplus.selenium.Page
import ui.utils.LoginPage
import ui.utils.UITestPage
import org.openqa.selenium.WebDriver
import scala.util.Random
import org.scalatest.time.Span
import org.scalatest.time.Second
import org.scalatest.time.Seconds

class RegisterSocietyTest extends UISpec {

  "Clicked on register society" should
    "take the user to the register society page" in {
      RegisterPage(host).navigate()
      currentUrl.contains("register-your-society") shouldBe (true)
    }

  "Adding a name and submitting" should "create a society" in {
    RegisterPage(host).navigate()
    textField("name").value = new Random(1234).alphanumeric.take(10).mkString
    find(className("mdc-button")).map { el =>
      executeScript("arguments[0].scrollIntoView(true);", el.underlying)
      implicitlyWait(Span(2, Seconds))
      el.underlying.click()
    }
    currentUrl.contains("select-society")
  }

}

case class RegisterPage(host: String)(implicit
    webDriver: WebDriver
) extends UITestPage(host) {
  val url: String = host + "register-your-society"
  def navigate(): Unit = {
    LoginPage(host).navigate()
    clickOn(className("mdc-button"))
  }
}
