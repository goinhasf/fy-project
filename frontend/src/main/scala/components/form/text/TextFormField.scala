package components.form.text

import org.scalajs.dom.html
import components.input.validation.InputValidation
import components.input.validation.NoValidation
import com.raquo.laminar.api.L._
import components.input.InputError
import components.form.FormField
import components.input.text.TextField
import components.form.FormEvents
import components.form.FormSubmitted
import components.form.FormResultReceived

class TextFormField[R](
    name: String,
    textField: TextField[html.Input],
    validation: InputValidation[String] = NoValidation()
)(implicit formEventBus: EventBus[FormEvents[R]])
    extends FormField[String, R](name, textField, validation)(formEventBus) {

  textField
    .textFieldContent
    .inputElement
    .amend(
      onChange.mapToValue --> matchValidate _,
      onFocus.mapToValue --> matchValidate _,
      onBlur.mapToValue --> matchValidate _
    )

  private def matchValidate(value: String) = {
    val result = validation.validate(value)
    result match {
      case Left(value) => textField.setInvalid(value.message)
      case Right(value) => {
        textField.setValid()
      }
    }
  }
}
