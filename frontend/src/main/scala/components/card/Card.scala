package components.card

import com.raquo.laminar.api.L._
import components.AutoInit
import components.MDCComponent
import components.MDCRipple
import components.MaterialComponent
import components.button
import components.button.MaterialButton
import components.button.label.ButtonLabel
import components.button.label.TextButtonLabel
import components.button.style.ButtonStyles
import components.id.ComponentID
import components.style.ComponentStyle
import org.scalajs.dom.document
import org.scalajs.dom.raw.UIEvent

import scalajs.js.constructorOf
import com.github.uosis.laminar.webcomponents.material

class Card(
    mTitle: String,
    mSubTitle: String = "",
    val cardContent: Option[HtmlElement] = None
) extends MaterialComponent[MDCComponent] {

  val title: Var[String]    = Var(mTitle)
  val subTitle: Var[String] = Var(mSubTitle)

  val classes: Setter[HtmlElement] =
    cls := Map(Card.elevated -> true, Card.outlined -> false)

  private var modifiers: Seq[Mod[HtmlElement]] = List.empty

  override lazy val rootElement: HtmlElement = div(classes, modifiers)

  def headerElement(): HtmlElement = div(
    cls := "mdc-card-wrapper__text-section",
    div(cls := "mdc-card__title", child.text <-- title.signal),
    div(cls := "mdc-card__subtitle", child.text <-- subTitle.signal)
  )

  def cardElements(): Seq[HtmlElement] = cardContent match {
    case None => Seq(headerElement())
    case Some(content) =>
      Seq(
        headerElement(),
        div(cls := "mdc-card-wrapper__text-section", content)
      )
  }

  def modify(mods: Mod[HtmlElement]*): Card = {
    this.modifiers = this.modifiers :++ mods.toSeq
    this
  }

  override def render(): HtmlElement = super
    .render()
    .amend(cardElements())
}

object Card {
  private val elevated: String = "mdc-card"
  val outlined: String         = "mdc-card--outlined"

  object CardHeader {
    private val header     = div(cls("mdc-card-wrapper__text-section"))
    private val titleEl    = div(cls := "mdc-card__title")
    private val subtitleEl = div(cls := "mdc-card__subtitle")

    def title(s: String) = {
      titleEl.amend(s)
    }
    def subtitle(s: String) = {
      subtitleEl.amend(s)
    }

    def apply(mods: CardHeader.type => Mod[HtmlElement]*) = {
      header
        .amend(mods.map(_(CardHeader)): _*)
    }
  }

  object Content {

    def media(url: String): HtmlElement = div(
      cls := "mdc-card-wrapper__text-section mdc-card__media mdc-card__media--16-9",
      backgroundImage(s"""url("$url")""")
    )

    def apply(el: HtmlElement*): HtmlElement =
      div(cls := "mdc-card-wrapper__text-section", el)
  }

  private val mainEl = div(cls(elevated))

  def apply(mods: Card.type => Mod[HtmlElement]*) = mainEl
    .amend(mods.map(_(Card)): _*)

}

class ActionableCard(
    mTitle: String,
    mSubTitle: String,
    override val cardContent: Option[HtmlElement],
    mActions: List[CardActionButton]
) extends Card(mTitle, mSubTitle, cardContent) {

  val actions: Var[List[CardActionButton]] = Var(mActions)

  lazy val actionsElement: HtmlElement = div(
    cls := "mdc-card__actions",
    children <-- actions
      .signal
      .map(_.map(_.render()))
  )

  override def cardElements(): Seq[HtmlElement] =
    super.cardElements() ++ Seq(actionsElement)

}

case class CardActionButton(
    title: String,
    val bAction: ComponentID => Unit = _ => ()
) extends MaterialButton {

  override val action: Var[ComponentID => Unit] = Var(bAction)

  override val buttonStyle: ComponentStyle = ButtonStyles.cardActionButtonStyle

  override val buttonLabel: ButtonLabel = TextButtonLabel(title)

}
