package data.auth

import org.scalatest.funspec.AnyFunSpec
import io.circe.parser._
import io.circe.syntax._

import dao.users.UserInfo
import org.scalatest.matchers.should.Matchers
import shared.auth.Role

class DecodingUserTest extends AnyFunSpec with Matchers {
  it("should decode the user json correctly") {

    val jsonUser =
      """
    {
        "id": "6061f0879d58f79b68308c35",
        "firstName": "Test",
        "lastName": "User",
        "email": "test@tester.com",
        "role": {
            "roleType": { "_t": "RegularUserRole" },
            "privileges": []
        }
    }
    
    """

    decode[UserInfo](jsonUser) shouldBe Right(
      UserInfo(
        "6061f0879d58f79b68308c35",
        "Test",
        "User",
        "test@tester.com",
        Role.RegularUser(Set.empty)
      )
    )
  }
}
