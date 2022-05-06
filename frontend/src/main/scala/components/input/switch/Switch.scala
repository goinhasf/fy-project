package components.input.switch

import components.MaterialComponent
import components.MDCSwitch
import components.id.Identifiable
import com.raquo.laminar.api.L
import components.id.ComponentID
import com.raquo.laminar.api.L._
import components.input.InputComponent
import org.scalajs.dom
import components.input.MaterialInput
import com.raquo.laminar.nodes.ReactiveHtmlElement
import components.card.Card

class Switch(labelText: String, isDefaultChecked: Boolean = false)
    extends MaterialInput[Boolean, MDCSwitch, dom.html.Input]
    with Identifiable {

  val id: ComponentID = ComponentID("mdc-switch")

  protected val checkedVar = Var(false)

  lazy val inputElement = input(
    typ := "checkbox",
    idAttr := id.toString(),
    cls := "mdc-switch__native-control",
    role := "switch",
    defaultChecked := isDefaultChecked,
    aria.checked <-- checkedVar.signal.map(_.toString()),
    onClick.mapToChecked --> checkedVar
  )
  protected val rootElement: L.HtmlElement = span(
    display := "flex",
    alignItems := "center",
    label(forId := id.toString(), labelText),
    div(
      cls := "mdc-switch",
      marginLeft := "auto",
      div(cls := "mdc-switch__track"),
      div(
        cls := "mdc-switch__thumb-underlay",
        div(cls := "mdc-switch__thumb"),
        inputElement
      ),
      onMountCallback(ctx =>
        mdcComponent.set(Some(new MDCSwitch(ctx.thisNode.ref)))
      )
    )
  )

  def getInputElement(): ReactiveHtmlElement[dom.html.Input] = inputElement

  def getValue(): Var[Boolean] = checkedVar
  def clearInput(): Unit  = checkedVar.set(isDefaultChecked)

}
