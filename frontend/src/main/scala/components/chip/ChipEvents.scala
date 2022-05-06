package components.chip

sealed trait ChipEvent
case class ChipClicked(id: Chip) extends ChipEvent
case class TrailingIconClicked() extends ChipEvent
case class DeleteChip(id: Chip)  extends ChipEvent
case class ChipSelected(chip: Chip, selected: Boolean) extends ChipEvent
