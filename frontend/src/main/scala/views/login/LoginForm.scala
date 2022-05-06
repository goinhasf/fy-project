package views.login

import components.form.MaterialForm
import com.raquo.laminar.api.L._
import org.scalajs.dom.experimental.HttpMethod
import components.button.MaterialButton
import components.button.label.TextButtonLabel
import components.button.style.ButtonStyles
import components.form.FormField
import components.input.text.OutlinedTextField
import components.input.InputError
import components.form.Form
import components.form.text.TextFormField
import components.input.validation.Required
import shared.auth.UserCredentialsAuth
import components.form.JsonFormPart
import services.ClientAuthenticationService
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
case class LoginForm() extends MaterialForm[Option[String]] {

  val submitMethod: HttpMethod = HttpMethod.POST
  val submitElement: HtmlElement = MaterialButton(
    TextButtonLabel("Login"),
    ButtonStyles.raisedButtonStyle
  ).amend(typ := "submit")

  val emailField = new TextFormField(
    "Email",
    new OutlinedTextField("Email"),
    Required(_.isEmpty())
  )

  val passwordField = new TextFormField(
    "password",
    new OutlinedTextField("Password")
      .editContent(_.inputElement.amend(typ := "password")),
    Required(_.isEmpty())
  )

  val formFields = Seq(
    emailField.render,
    passwordField.render
  )

  val encodingType: String = "multipart/form-data"

  def toForm(): Either[InputError, Form] = {
    for {
      email    <- emailField.validateTransform()
      password <- passwordField.validateTransform()
      credentials = UserCredentialsAuth(email, password)
    } yield Form(JsonFormPart("credentials", credentials.asJson))
  }

  def getCredentials = for {
    email    <- emailField.validateTransform()
    password <- passwordField.validateTransform()
  } yield UserCredentialsAuth(email, password)

  def submit(form: Form): EventStream[Option[String]] = {
    getCredentials match {
      case Left(value)  => EventStream.fromValue(None, false)
      case Right(value) => ClientAuthenticationService.login(value)
    }
  }

}
