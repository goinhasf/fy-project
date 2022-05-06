package components.input.text

import com.raquo.laminar.api.L._
import com.raquo.domtypes.generic.codecs.StringAsIsCodec
import components.Material

trait HelperTextComponent {
  val helperText: HelperText
}

class HelperText(initialHelperText: String) extends Material {

  protected val baseStyle = Seq(
    "mdc-text-field-helper-text",
    "mdc-text-field-helper-text--persistent"
  )

  val errorColour  = "var(--mdc-theme-error, #b00020)"
  val normalColour = "rgba(0,0,0,.6)"

  val styles                          = Var(baseStyle)
  val helperText: Var[String]         = Var(initialHelperText)
  val roleAttr                        = new HtmlAttr[String]("role", StringAsIsCodec)
  private val textColour: Var[String] = Var(normalColour)

  def render(): HtmlElement = div(
    cls := "mdc-text-field-helper-line",
    div(
      aria.hidden := true,
      cls <-- styles.signal,
      child.text <-- helperText.signal,
      roleAttr := "alert",
      color <-- textColour.signal
    )
  )

  def makeError(err: String): Unit = {
    styles.set(baseStyle :+ "mdc-text-field-helper-text--validation-msg")
    helperText.set(err)
    textColour.set(errorColour)
  }

  def clearError: Unit = {
    styles.set(baseStyle)
    helperText.set(initialHelperText)
    textColour.set(normalColour)
  }

}
class NoHelperText extends HelperText("") {
  override def render(): HtmlElement = super.render().amend(display.none)
}
object HelperText {
  val NoHelperText = new NoHelperText()
}
