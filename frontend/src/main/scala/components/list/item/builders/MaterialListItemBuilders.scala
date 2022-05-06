package components.list.item.builders

import com.raquo.laminar.api.L._
import components.MaterialComponent.MaterialComponentAttributes
import components.id.Identifiable

trait ListItemTextElement {
  def textElement: HtmlElement
}

trait SingleLineTextElement extends ListItemTextElement {
  val textVar: Var[String]
  override def textElement: HtmlElement =
    span(
      cls := "mdc-list-item__text",
      child.text <-- textVar.signal
    )

}

trait TwoLineListItem extends ListItemTextElement {
  val primaryTextVar: Var[String]
  val secondaryTextVar: Var[String]

  override def textElement: HtmlElement =
    span(
      cls := "mdc-list-item__text",
      span(
        cls := "mdc-list-item__primary-text",
        child.text <-- primaryTextVar.signal
      ),
      span(
        cls := "mdc-list-item__secondary-text",
        child.text <-- secondaryTextVar.signal
      )
    )

}

trait BeforeListItemElement {
  val beforeListElement: HtmlElement
}

trait EmptyBeforeListElement extends BeforeListItemElement {
  override lazy val beforeListElement: HtmlElement = span()
}

trait BeforeListItemIcon
    extends BeforeListItemElement
    with MaterialComponentAttributes {
  val beforeListItemIcon: Var[String]
  override lazy val beforeListElement: HtmlElement = span(
    cls := "mdc-list-item__graphic material-icons md-dark",
    marginTop := "auto",
    marginBottom := "auto",
    paddingRight := "1rem",
    ariaHidden := "true",
    child.text <-- beforeListItemIcon.signal
  )
}

trait AfterListItemElement extends MaterialComponentAttributes {
  val afterListElement: HtmlElement
}

trait EmptyAfterListElement extends AfterListItemElement {
  override lazy val afterListElement: HtmlElement = span()
}

trait AfterListItemIcon extends AfterListItemElement with Identifiable {
  val afterListItemIcon: Var[String]
  override lazy val afterListElement: HtmlElement = span(
    cls := "mdc-list-item__meta",
    ariaHidden := "true",
    marginTop := "auto",
    marginBottom := "auto",
    marginLeft := "auto",
    button(
      cls := "mdc-icon-button material-icons md-dark",
      child.text <-- afterListItemIcon.signal
    )
  )

}

trait AfterListText extends AfterListItemElement {
  val afterListText: Var[String]
  override lazy val afterListElement: HtmlElement = span(
    cls := "mdc-list-item__meta",
    ariaHidden := "true",
    child.text <-- afterListText.signal
  )

}
