package components.button.label

import components.MaterialComponent.MaterialComponentAttributes
import com.raquo.laminar.api.L._

sealed trait ButtonLabel extends MaterialComponentAttributes {
  val label: Var[Option[String]]
  def render(): Seq[HtmlElement]
}
case class TextButtonLabel(bLabel: String) extends ButtonLabel {

  override val label: Var[Option[String]] = Var(Some(bLabel))

  def render(): Seq[HtmlElement] = Seq(
    span(
      cls := "mdc-button__label",
      child.text <-- label.signal.map(_.getOrElse(""))
    )
  )
}
case class IconButtonLabel(bLabel: String, mIcon: String) extends ButtonLabel {

  private val textButtonLabel: TextButtonLabel = TextButtonLabel(bLabel)

  override val label: Var[Option[String]] = textButtonLabel.label
  val icon: Var[String]                   = Var(mIcon)

  def render(): Seq[HtmlElement] =
    i(
      cls := "material-icons mdc-button__icon",
      ariaHidden := "true",
      icon.tryNow().toOption
    ) +: textButtonLabel.render()
}

case class FloatingActionButtonLabel(icon: Var[String]) extends ButtonLabel {

  override val label: Var[Option[String]] = Var(None)

  override def render(): Seq[HtmlElement] = Seq(
    span(
      cls := "material-icons mdc-button__icon",
      child.text <-- icon.signal
    )
  )

}
