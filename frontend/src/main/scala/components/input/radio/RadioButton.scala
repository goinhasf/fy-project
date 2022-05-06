package components.input.radio

import components.input.MaterialInput
import com.raquo.laminar.api.L._
import org.scalajs.dom
import components.MDCRadio
import com.raquo.laminar.nodes.ReactiveHtmlElement
import components.id.Identifiable
import components.id.ComponentID

case class RadioButton(labelText: String, defaultChecked: Boolean = true)
    extends MaterialInput[Boolean, MDCRadio, dom.html.Input]
    with Identifiable {
  val id: ComponentID = ComponentID("mdc-radio")

  private val inputValue = Var(defaultChecked)

  protected val rootElement: HtmlElement = {
    div(
      cls := "mdc-form-field",
      div(
        cls := "mdc-radio",
        getInputElement(),
        div(
          cls := "mdc-radio__background",
          div(cls := "mdc-radio__outer-circle"),
          div(cls := "mdc-radio__inner-circle")
        ),
        div(cls := "mdc-radio__ripple")
      ),
      label(forId := id.toString(), labelText)
    )
  }

  def getInputElement(): ReactiveHtmlElement[dom.html.Input] =
    input(
      cls := "mdc-radio__native-control",
      typ := "radio",
      idAttr := id.toString(),
      name := "radios",
      checked := defaultChecked,
      onInput.mapToChecked --> inputValue
    )

  def getValue(): Var[Boolean] = inputValue

  def clearInput(): Unit = {
    getInputElement().amend(checked := defaultChecked)
    inputValue.set(defaultChecked)
  }

}
