package components.form

import components.MDCComponent
import com.raquo.laminar.api.L._
import components.Material
import org.scalajs.dom.html
import com.raquo.laminar.nodes.ReactiveHtmlElement
import components.style.ComponentStyle
import components.input.InputComponent
import components.input.validation.{InputValidation, InputValidationComponent}
import components.input.validation.NoValidation
import components.input.MaterialInput
import components.MaterialComponent

class FormField[A, R](
    val fieldName: String,
    component: FormField.ComponentType[A],
    validation: InputValidation[A] = NoValidation[A]()
)(implicit formEventBus: EventBus[FormEvents[R]])
    extends MaterialInput[A, MDCComponent, html.Element] {

  private val inputElement: ReactiveHtmlElement[_ <: html.Element] =
    component
      .getInputElement()
      .amend(
        name := fieldName,
        formEventBus
          .events
          .collect({ case FormResultReceived(result) => result }) --> { _ =>
          component.clearInput()
        }
      )

  def validateTransform() = validation
    .validate(component.getValue().now())

  protected val rootElement: HtmlElement =
    div(paddingTop := "1rem", component.render())

  def getInputElement(): ReactiveHtmlElement[html.Element] = inputElement
  def getValue(): Var[A]                                   = component.getValue()
  def clearInput(): Unit                                   = component.clearInput()
}

object FormField {
  type ComponentType[A] = MaterialComponent[_ <: MDCComponent]
    with InputComponent[A, _ <: html.Element]
}
