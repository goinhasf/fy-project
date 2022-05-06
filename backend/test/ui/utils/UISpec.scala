package ui.utils

import org.scalatestplus.selenium.WebBrowser
import org.openqa.selenium.WebDriver
import org.openqa.selenium.firefox.FirefoxDriver
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.selenium.Page
import org.openqa.selenium

trait UISpec
    extends AnyFlatSpec
    with Matchers
    with BeforeAndAfterAll
    with WebBrowser {
  implicit val webDriver: WebDriver = new FirefoxDriver
  val host                          = "http://localhost:8080/"

  webDriver.manage().window().setSize(new selenium.Dimension(360, 740))

  def login()(implicit webDriver: WebDriver) = {
    LoginPage(host).navigate()
  }

  override protected def afterAll(): Unit = close()

}

abstract class UITestPage(host: String)(implicit
    webDriver: WebDriver
) extends Page
    with WebBrowser {
  def navigate(): Unit
}

case class LoginPage(host: String)(implicit webDriver: WebDriver)
    extends UITestPage(host) {
  val url: String = host + "login"
  def navigate(): Unit = {
    go to url
    textField(xpath("//input[@type='text']")).value = "regular-user@user.com"
    clickOn("password")
    find("password").map(_.underlying.sendKeys("password"))
    clickOn(className("mdc-button"))
  }
}
