package components.form

import components.MaterialComponent
import components.MDCComponent
import com.raquo.laminar.api.L._
import org.scalajs.dom.experimental.HttpMethod
import components.id.Identifiable
import components.id.ComponentID
import org.scalajs.dom.raw.FormData

import scala.util.Try
import com.raquo.airstream.ownership.OneTimeOwner
import components.MDCDialog
import scala.util.Failure
import scala.util.Success
import components.input.InputError
import org.scalajs.dom.ext.KeyCode
import java.awt.event.KeyEvent
import components.Material
import org.scalajs.dom
import components.input.InputComponent
import components.input.MaterialInput

trait MaterialForm[A]
    extends MaterialComponent[MDCComponent]
    with Identifiable {

  val submitMethod: HttpMethod
  val submitElement: HtmlElement
  val formFields: Seq[HtmlElement]
  val encodingType: String

  implicit val eventBus: EventBus[FormEvents[A]] = new EventBus[FormEvents[A]]
  val actionString: Option[String]               = None
  val errorVar                                   = Var[String]("")
  val id: ComponentID                            = ComponentID("mdc-form")

  override protected lazy val rootElement: HtmlElement = {
    form(
      idAttr := id.toString(),
      cls := "mdc-form",
      method := submitMethod.toString(),
      encType := encodingType,
      actionString
        .map(actionS => action := actionS)
        .getOrElse(emptyMod),
      formFields,
      div(cls := "form-errors", child.text <-- errorVar.signal),
      submitElement,
      handleSubmit()
    )
  }

  private def handleSubmit() =
    onSubmit.preventDefault --> { _ =>
      eventBus.emit(FormSubmitted())
      toForm().fold(
        errors => {
          errorVar.set("Form has errors")
          eventBus.emit(FormInputErrorEvent(errors))
        },
        form => {
          rootElement.amend(
            submit(form).map(r => FormResultReceived(r)) --> eventBus
          )
          errorVar.set("")
        }
      )
    }

  def toForm(): Either[InputError, Form]
  def submit(form: Form): Observable[A]
}
