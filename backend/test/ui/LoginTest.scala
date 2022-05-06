package ui

import ui.utils.UISpec

class LoginTest extends UISpec {

  "The login page" should "have the correct title" in {
    go to (host + "login")
    pageTitle should be("Society Management")
  }

  "The login page" should "have the username and password inputs" in {
    go to (host + "login")
    clickOn(xpath("//input[@type='text']"))
    clickOn(xpath("//input[@type='password']"))
    findAll(tagName("input")).size should equal(2)
  }

  "Clicking login with no credentials" should "show an error message" in {
    go to (host + "login")
    clickOn(className("mdc-button"))
    find(className("form-errors")) should matchPattern({ case Some(v) => })
  }

  "Clicking login with the right credentials" should "take the user to the select society page" in {
    login()
    currentUrl.contains("select-society") shouldBe (true)

  }

}
