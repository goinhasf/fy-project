package views.registerSociety.form

import com.raquo.laminar.api.L._
import components.button.MaterialButton
import components.button.label.TextButtonLabel
import components.button.style.ButtonStyles
import components.form.Form
import components.form.FormField
import components.form.MaterialForm
import components.form.file.FileInputFormField
import components.input.InputError
import components.input.file.OutlinedFileInputElement
import components.input.text.OutlinedTextField
import components.input.validation.Required
import dao.societies.Society
import endpoints4s.Invalid
import org.scalajs.dom.experimental.HttpMethod
import services.SocietiesService
import components.form.JsonFormPart
import dao.societies.SocietyDetails
import io.circe.syntax._
import components.form.FileFormPart
import components.form.FormPart

class SocietyRegistrationForm extends MaterialForm[Either[Invalid, Society]] {

  override val submitMethod: HttpMethod = HttpMethod.POST

  override val submitElement: HtmlElement = MaterialButton(
    TextButtonLabel("Submit"),
    ButtonStyles.raisedButtonStyle
  )

  val nameField = new FormField(
    "name",
    new OutlinedTextField("Society Name").editRoot(el =>
      el.getInputElement().amend(required(true))
    ),
    Required[String](_.isEmpty())
  )

  val descriptionField = new FormField(
    "description",
    new OutlinedTextField("Society Description")
  )

  val pictureField = new FileInputFormField(
    "file",
    new OutlinedFileInputElement("Society Photo", "Max size 200px by 200px")
  )

  val societyEmailField = new FormField(
    "email",
    new OutlinedTextField("Society Email")
  )

  val societyWebPageField = new FormField(
    "webPageUrl",
    new OutlinedTextField("Web Page URL")
  )

  val facebookPageField = new FormField(
    "facebookPageUrl",
    new OutlinedTextField("Facebook Page URL")
  )

  override val formFields: Seq[HtmlElement] = Seq(
    nameField,
    descriptionField,
    pictureField,
    societyEmailField,
    societyWebPageField,
    facebookPageField
  )

  override val encodingType: String = "multipart/form-data"

  override def toForm(): Either[InputError, Form] = for {
    name <- nameField.validateTransform()
    desc         = descriptionField.validateTransform().toOption
    picture      = pictureField.validateTransform().toOption.flatten
    email        = societyEmailField.validateTransform().toOption
    webPage      = societyWebPageField.validateTransform().toOption
    facebookPage = facebookPageField.validateTransform().toOption
  } yield {

    val parts = picture
      .toList
      .map(file => FileFormPart("file", file)) :+ JsonFormPart(
      "details",
      SocietyDetails(name, desc, email, webPage, facebookPage).asJson
    )

    Form(parts)
  }

  override def submit(form: Form): Observable[Either[Invalid, Society]] = {
    SocietiesService.createSociety(form)
  }

}
