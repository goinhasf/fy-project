package data.auth

import org.mockito.IdiomaticMockito
import org.mockito.ArgumentMatchersSugar
import utils.TokenHashing
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import utils.TokenHashing._
class PasswordComparisonTest
    extends AnyWordSpec
    with Matchers
    with IdiomaticMockito
    with ArgumentMatchersSugar {

  "Comparing passwords" should {
    "succeed if they are identical" in {
      val clearTextPassword = "password"
      val password          = clearTextPassword.toPassword
      println(password)
      password.compareString(clearTextPassword) mustBe Some(password)
    }

    "return None if they are not" in {
      val clearTextPassword = "password"
      val password          = clearTextPassword.toPassword
      password.compareString("anotherPassword") mustBe None
    }
  }

}
