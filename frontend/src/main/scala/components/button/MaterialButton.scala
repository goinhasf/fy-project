package components.button

import components.MaterialComponent
import com.raquo.laminar.api.L._
import org.scalajs.dom
import components.MaterialComponent.MaterialComponentAttributes
import components.ripple.Ripple
import components.id._
import components.MDCButton
import components.style.ComponentStyle
import components.button.label.ButtonLabel
import org.scalajs.dom.raw.Event

trait MaterialButton
    extends MaterialComponent[MDCButton]
    with Ripple
    with Identifiable {

  override val id: ComponentID = ComponentID("mdc-button")

  val buttonStyle: ComponentStyle
  val buttonLabel: ButtonLabel
  val action: Var[ComponentID => Unit]

  lazy val touchTarget: HtmlElement = span(cls := "mdc-button__touch")
  lazy val innerElements            = ripple +: buttonLabel.render() :+ touchTarget

  override lazy val rootElement: HtmlElement = button(
    idAttr := id.toString(),
    cls <-- buttonStyle.stylesMap.signal,
    innerElements,
    onClick.mapTo(id) --> action.now()
  )
}
object MaterialButton {

  def apply(
      bLabel: ButtonLabel,
      bStyle: ComponentStyle,
      bAction: ComponentID => Unit = _ => ()
  ): MaterialButton = new MaterialButton {

    override val action = Var(bAction)

    override val buttonStyle: ComponentStyle = bStyle

    override val buttonLabel: ButtonLabel = bLabel

  }

  final class AccessibleMaterialButton(button: MaterialButton) { self =>
    def makeAccessible(): MaterialButton = new MaterialButton {

      override val buttonStyle: ComponentStyle = button.buttonStyle

      override val buttonLabel: ButtonLabel = button.buttonLabel

      override val action = button.action

      override lazy val rootElement: HtmlElement =
        div(cls := "mdc-touch-target-wrapper", button.rootElement)

    }

  }

}
