package components.chip
import com.raquo.laminar.api.L._
import components.MaterialComponent
import components.id.Identifiable
import components.ripple.Ripple
import components.id.ComponentID
import components.MDCChip
import org.scalajs.dom.raw.UIEvent
import org.scalajs.dom.raw.SVGPathSeg
import com.raquo.laminar.nodes.ReactiveHtmlElement
import components.MDCComponent

class Chip(
    value: String,
    val onClickFn: Unit => Unit = _ => {},
    val leadingIcon: Option[ChipIconLeading] = None,
    val trailingIcon: Option[ChipIconTrailing] = None
) extends MaterialComponent[MDCChip]
    with Identifiable
    with Ripple {

  val eventBus: EventBus[ChipEvent] = new EventBus[ChipEvent]
  protected def internalChipObserver = Observer[ChipEvent] { event =>
    event match {
      case ChipClicked(id)              => onClickFn()
      case TrailingIconClicked()        => eventBus.emit(DeleteChip(this))
      case DeleteChip(id)               => mdcComponent.now().map(_.beginExit())
      case ChipSelected(chip, selected) => {}
    }
  }
  val valueVar: Var[String] = Var(value)

  val buttonRole: String = "button"
  def action: HtmlElement = span(
    role := buttonRole,
    cls := "mdc-chip__primary-action",
    span(cls := "mdc-chip__text", child.text <-- valueVar.signal)
  )

  val id: ComponentID = ComponentID("mdc-chip")
  lazy val rootElement: HtmlElement = div(
    idAttr := id.toString(),
    cls := "mdc-chip",
    role := "row",
    ripple,
    span(
      leadingIcon.map(_.render()),
      role := "gridcell",
      action
    ),
    trailingIcon.map(_.render().amend(idAttr := id.toString())),
    trailingIcon
      .map(_.eventBus.events --> eventBus)
      .getOrElse(emptyMod),
    onClick.mapTo(ChipClicked(this)) --> eventBus,
    eventBus --> internalChipObserver,
    onMountCallback(ctx =>
      mdcComponent.set(Some(new MDCChip(ctx.thisNode.ref)))
    )
  )

  def destroy(): Unit = {
    eventBus.emit(DeleteChip(this))
  }

  override def equals(o: Any): Boolean = {
    if (o.isInstanceOf[this.type]) {
      o.asInstanceOf[this.type].valueVar.now() == this.valueVar.now()
    } else {
      false
    }
  }
}

class FilterChip(
    value: String,
    override val onClickFn: Unit => Unit = _ => {},
    override val leadingIcon: Option[ChipIconLeading] = None,
    override val trailingIcon: Option[ChipIconTrailing] = None
) extends Chip(value, onClickFn, leadingIcon, trailingIcon) {

  val selectedVar: Var[Boolean]   = Var(false)
  override val buttonRole: String = "checkbox"

  override protected def internalChipObserver: Observer[ChipEvent] =
    Observer[ChipEvent] { event =>
      event match {
        case ChipSelected(chip, selected) => {
          mdcComponent
            .now()
            .map(chip => chip.setSelectedFromChipSet(selected))
          selectedVar.set(selected)
        }
        case otherEvents: ChipEvent =>
          super.internalChipObserver.onNext(otherEvents)
      }
    }

  override def render(): HtmlElement = div(
    idAttr := id.toString(),
    cls := "mdc-chip",
    role := "row",
    ripple,
    leadingIcon.map(_.render().amend(cls := "mdc-chip__icon--leading-hidden")),
    new CheckMarkIcon().render(),
    span(
      role := "gridcell",
      action.amend(aria.checked <-- selectedVar.signal.map(_.toString()))
    ),
    trailingIcon.map(_.render().amend(idAttr := id.toString())),
    trailingIcon.map(_.eventBus.events --> eventBus).getOrElse(emptyMod),
    onClick.mapTo(ChipClicked(this)) --> eventBus,
    onClick.mapTo(ChipSelected(this, !selectedVar.now())) --> eventBus,
    eventBus --> internalChipObserver,
    onMountCallback(ctx =>
      mdcComponent.set(Some(new MDCChip(ctx.thisNode.ref)))
    )
  )

}

object FilterChip {
  case class FilterChipSelectedEvent(chip: FilterChip)
}

class CheckMarkIcon() extends ChipIconLeading("") {
  override def render(): HtmlElement =
    span(
      svg.svg(
        svg.cls := "mdc-chip__checkmark-svg",
        svg.viewBox := "-2 -3 30 30",
        svg.path(
          svg.cls := "mdc-chip__checkmark-path",
          svg.fill := "none",
          svg.stroke := "black",
          svg.d := "M1.73,12.91 8.1,19.28 22.79,4.59"
        )
      )
    )

}

class ChipIcon(val icon: String) extends MaterialComponent[MDCComponent] {

  lazy val rootElement: HtmlElement = i(
    cls := baseStyles.fold("")((a, b) => s"$a $b"),
    icon
  )

  protected def baseStyles(): Seq[String] =
    Seq("material-icons", "mdc-chip__icon", "md-dark")

}

class ChipIconLeading(icon: String) extends ChipIcon(icon) {
  override protected def baseStyles(): Seq[String] =
    super.baseStyles :+ "mdc-chip__icon--leading"
}

class ChipIconTrailing() extends ChipIcon("cancel") {

  val eventBus: EventBus[ChipEvent] = new EventBus[ChipEvent]()

  override protected def baseStyles(): Seq[String] =
    super.baseStyles :+ "mdc-chip__icon--trailing"

  override lazy val rootElement: HtmlElement =
    span(
      role := "gridcell",
      i(
        cls := baseStyles.fold("")((a, b) => s"$a $b"),
        icon,
        role := "button"
      ),
      onClick.mapTo(TrailingIconClicked()) --> eventBus
    )

}
