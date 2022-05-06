package components.editable

import com.raquo.airstream.eventbus.EventBus
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import components.MDCComponent
import components.Material
import components.MaterialComponent
import components.form.FormField
import components.input.MaterialInput
import org.scalajs.dom.html
import components.card.Card
import components.card.ActionableCard
import components.card.CardActionButton

class EditableField[
    A,
    M <: MDCComponent,
    +Ref <: html.Element,
    C <: MaterialInput[
      A,
      M,
      Ref
    ]
](
    val label: String,
    val editComponent: C,
) extends MaterialInput[A, MDCComponent, Ref] {

  protected val rootElement: HtmlElement = div(
    cls := "editable-field",
    span(
      label
    ),
    div(
      editComponent
    )
  )
  override def getInputElement(): ReactiveHtmlElement[Ref] = editComponent
    .getInputElement()

  override def getValue(): Var[A] = editComponent.getValue()
  override def clearInput(): Unit = editComponent.clearInput()
}
