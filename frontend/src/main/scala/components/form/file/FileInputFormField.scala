package components.form.file

import components.form.FormField
import components.input.file.OutlinedFileInputElement
import components.input.validation.InputValidation
import org.scalajs.dom.raw.File
import components.input.validation.NoValidation
import com.raquo.laminar.api.L._
import org.scalajs.dom.raw.Event
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import components.form.FormEvents
import components.form.FormSubmitted
import components.form.FormResultReceived

class FileInputFormField[R](
    fieldName: String,
    fileInput: OutlinedFileInputElement,
    validation: InputValidation[Option[File]] = NoValidation[Option[File]]()
)(implicit formEventBus: EventBus[FormEvents[R]])
    extends FormField[Option[File], R](fieldName, fileInput, validation) {

  fileInput
    .inputFieldContent
    .inputElement
    .amendThis(thisNode => addEventProcessor(onSelect, thisNode))
    .amendThis(thisNode => addEventProcessor(onClick, thisNode))
    .amendThis(thisNode => addEventProcessor(onChange, thisNode))

  private def addEventProcessor(
      ep: EventProp[_ <: Event],
      thisNode: ReactiveHtmlElement[dom.html.Input]
  ) = {

    ep --> { _ =>
      val maybeFile = thisNode.ref.files(0)
      val f         = if (scalajs.js.isUndefined(maybeFile)) None else Some(maybeFile)
      matchValidate(f)
    }
  }

  private def matchValidate(value: Option[File]) = {
    val result = validation.validate(value)
    result match {
      case Left(value)  => fileInput.setInvalid(value.message)
      case Right(value) => fileInput.setValid()
    }
  }
}
