package components.input.text.content

import com.raquo.laminar.api.L._
import org.scalajs.dom.html
import components.MaterialComponent
import components.id.Identifiable
import components.id.ComponentID
import components.ripple.Ripple
import com.raquo.laminar.nodes.ReactiveHtmlElement
import components.style.ComponentStyle

case class FloatingLabel(id: ComponentID, text: String) extends ComponentStyle {
  val initialStyles: Seq[String] = Seq("mdc-floating-label")
  lazy val label: HtmlElement =
    span(cls <-- stylesMap, idAttr := id.toString(), text)
}

abstract class TextFieldContent[+Ref <: html.Element](
    val label: FloatingLabel
) extends MaterialComponent.MaterialComponentAttributes {
  val valueVar: Var[String] = Var("")
  val inputElement: ReactiveHtmlElement[Ref]
  def render(): Mod[HtmlElement]
}

case class FilledTextFieldContent(override val label: FloatingLabel)
    extends TextFieldContent[html.Input](label) {
  lazy val inputElement: ReactiveHtmlElement[html.Input] = input(
    cls := "mdc-text-field__input",
    typ := "text",
    ariaLabeledBy := label.id.toString(),
    onInput.mapToValue --> valueVar,
    value <-- valueVar.signal
  )
  def render(): Mod[HtmlElement] = Seq(
    label.label,
    inputElement,
    span(cls := "mdc-line-ripple")
  )
}

case class OutlinedTextFieldContent(override val label: FloatingLabel)
    extends TextFieldContent[html.Input](label) {

  lazy val inputElement: ReactiveHtmlElement[html.Input] = input(
    cls := "mdc-text-field__input",
    typ := "text",
    ariaLabeledBy := label.id.toString(),
    onInput.mapToValue --> valueVar,
    value <-- valueVar.signal
  )

  def render(): Mod[HtmlElement] = Seq(
    span(
      cls := "mdc-notched-outline",
      span(cls := "mdc-notched-outline__leading"),
      span(cls := "mdc-notched-outline__notch", label.label),
      span(cls := "mdc-notched-outline__trailing")
    ),
    inputElement
  )
}
