package components.input.text.styles

import components.style.ComponentStyle

trait FilledTextFieldStyle extends ComponentStyle {
  override val initialStyles: Seq[String] = TextFieldStyles.filledStyle
}

trait OutlinedTextFieldStyle extends ComponentStyle {
  override val initialStyles: Seq[String] = TextFieldStyles.outlinedStyle
}

object TextFieldStyles {
  val baseStyle: Seq[String] = Seq("mdc-text-field")

  val filledStyle   = "mdc-text-field--filled" +: baseStyle
  val outlinedStyle = "mdc-text-field--outlined" +: baseStyle

  val outlinedTextAreaStyle =
    outlinedStyle :+ "mdc-text-field--textarea" :+ "mdc-text-field--no-label"
  val filledTextAreaStyle =
    filledStyle :+ "mdc-text-field--textarea" :+ "mdc-text-field--no-label"
}
