package components.button.style

import com.raquo.laminar.api.L._
import components.style.ComponentStyle

object ButtonStyles {
  val baseStyleSeq = Seq("mdc-button", "mdc-button--touch")

  val baseButtonStyle: ComponentStyle = ComponentStyle(baseStyleSeq)

  val outlinedButtonStyle: ComponentStyle =
    ComponentStyle(baseStyleSeq :+ "mdc-button--outlined")

  val raisedButtonStyle: ComponentStyle =
    ComponentStyle(baseStyleSeq :+ "mdc-button--raised")

  val cardActionButtonStyle: ComponentStyle =
    ComponentStyle(
      baseStyleSeq :+ "mdc-card__action" :+ "mdc-card__action--button"
    )
  
  val floatingActionButtonStyle: ComponentStyle = ComponentStyle(
    Seq("mdc-fab", "mdc-fab--mini", "mdc-fab--touch", "mdc-fab--exited")
  )

  val dialogButtonStyle: ComponentStyle = ComponentStyle(
    baseStyleSeq :+ "mdc-dialog__button"
  )
}
