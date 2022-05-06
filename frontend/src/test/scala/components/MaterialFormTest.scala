package components

import org.scalatest.funspec.AnyFunSpec
import com.raquo.laminar.api.L._
import org.scalajs.dom
import org.scalatest.matchers.should.Matchers
import components.form.MaterialForm
import org.scalajs.dom.experimental.HttpMethod
import components.input.InputError
import components.form.Form
import components.form.JsonFormPart
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
import components.input.text.TextField
import components.input.validation.Required
import components.input.InputComponent
import com.raquo.laminar.nodes.ReactiveHtmlElement
import components.input.MaterialInput
import components.form.FormField
import components.form.FormResultReceived
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
class MaterialFormTest extends AnyFunSpec with Matchers {

  it("should submit a form") {

    val textField = new MaterialInput[String, MDCComponent, dom.html.Input] {

      val inputElement = input(typ := "text")

      lazy val rootElement: HtmlElement = inputElement

      def getInputElement(): ReactiveHtmlElement[org.scalajs.dom.html.Input] =
        inputElement

      def getValue(): Var[String] = Var(getInputElement().ref.value)

      def clearInput(): Unit = getInputElement().ref.value = ""

    }

    // A form field with the required attribute

    val resultVar = Var("")

    val materialForm = new MaterialForm[String] {

      val formField = new FormField(
        "test",
        textField,
        Required[String](_.isEmpty())
      )

      val submitMethod: HttpMethod     = HttpMethod.POST
      val submitElement: HtmlElement   = input(typ := "submit")
      val formFields: Seq[HtmlElement] = Seq(formField.render)
      val encodingType: String         = "multipart/form-data"

      def toForm(): Either[InputError, Form] = {
        formField
          .validateTransform()
          .map(formFieldValue =>
            Form(
              JsonFormPart(formField.fieldName, Json.fromString(formFieldValue))
            )
          )
      }

      def submit(form: Form): EventStream[String] = EventStream.fromValue(
        "Success",
        true
      )

      override def render(): HtmlElement = super
        .render()
        .amend(
          eventBus
            .events
            .collect({ case FormResultReceived(result) => result }
            ) --> resultVar
        )

    }
    // Check that form fails when input is required
    materialForm.toForm().isLeft shouldBe true

    // Set TextField to a value
    textField.inputElement.ref.value = "value"

    // Check that form validation has passed
    materialForm.toForm().isRight shouldBe true

    // Check that the data in the form was set correctly
    materialForm
      .toForm()
      .getOrElse(fail())
      .fileParts
      .head
      .value shouldBe s""""${textField.inputElement.ref.value}""""

    render(dom.document.body, materialForm)
    // Click Submit
    materialForm.submitElement.ref.click()
    resultVar.now() shouldBe "Success"
  }

}
