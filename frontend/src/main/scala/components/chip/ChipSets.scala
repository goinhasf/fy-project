package components.chip

import components.MaterialComponent
import components.MDCChipSet
import com.raquo.laminar.api.L._
import components.id.ComponentID
import components.id.Identifiable
import components.style.ComponentStyle
import com.raquo.airstream.ownership.OneTimeOwner

class BaseChipSet[T <: Chip](initialChips: Set[T] = Set.empty[T])
    extends MaterialComponent[MDCChipSet]
    with ComponentStyle
    with Identifiable {

  val chips: Var[Set[T]]         = Var(initialChips)
  val id: ComponentID            = ComponentID("mdc-chip-set")
  protected val baseStyle        = Seq("mdc-chip-set")
  val initialStyles: Seq[String] = baseStyle
  protected val internalChipObserver = Observer[ChipEvent] { event =>
    event match {
      case DeleteChip(chip) => {
        chips.update(currentChips =>
          currentChips.filterNot(_.id.toString() == chip.id.toString())
        )
      }
      case _ => {}
    }
  }

  protected lazy val rootElement: HtmlElement = div(
    idAttr := id.toString(),
    cls <-- stylesMap.signal,
    role := "grid",
    children <-- chips.signal.map(_.toSeq.map(_.render())),
    chipsEventStream --> internalChipObserver,
    onMountCallback(ctx => {
      val mdcChipSet = new MDCChipSet(ctx.thisNode.ref)
      mdcComponent.set(Some(mdcChipSet))
      initialChips.foreach(c => mdcChipSet.addChip(c.ref))
    })
  )

  def chipsEventStream = chips
    .signal
    .flatMap(chips => EventStream.mergeSeq(chips.toSeq.map(_.eventBus.events)))

  def addChip(chip: T, mods: Mod[HtmlElement]*): Unit = {
    // Check if the item already exists in the set
    if (!chips.now().contains(chip)) {
      chip.amend(mods)
      chips.update(_ + chip)
      mdcComponent.now().map(_.addChip(chip.rootElement.ref))
    }
  }

}

class FilterChipSet(initialChips: FilterChip*)
    extends BaseChipSet[FilterChip](initialChips.toSet) {
  override val initialStyles: Seq[String] = "mdc-chip-set--filter" +: baseStyle
  def getSelected(): Set[FilterChip] =
    chips.now().filter(_.selectedVar.now() == true)
}
