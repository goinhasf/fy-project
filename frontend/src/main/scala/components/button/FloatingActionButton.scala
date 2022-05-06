package components.button

import components.style.ComponentStyle
import components.button.style.ButtonStyles
import components.button.label.ButtonLabel
import components.button.label.FloatingActionButtonLabel
import com.raquo.laminar.api.L._
import components.id.ComponentID

case class FloatingActionButton(
    val bAction: ComponentID => Unit = _ => ()
) extends MaterialButton {

  override val action: Var[ComponentID => Unit] = Var(bAction)

  override val buttonStyle: ComponentStyle =
    ButtonStyles.floatingActionButtonStyle

  override val buttonLabel: ButtonLabel =
    FloatingActionButtonLabel(Var("add"))

  def show(): Unit = {
    buttonStyle
      .stylesMap
      .update(_ + ("mdc-fab--exited" -> false))
  }

  def hide(): Unit = {
    buttonStyle
      .stylesMap
      .update(_ + ("mdc-fab--exited" -> true))
  }
}
