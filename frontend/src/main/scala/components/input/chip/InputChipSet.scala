package components.input.chip

import components.chip.Chip
import components.chip.BaseChipSet
import com.raquo.laminar.api.L._
import components.input.text.TextField
import org.scalajs.dom.html
import components.input.MaterialInput
import com.raquo.laminar.nodes.ReactiveHtmlElement
import components.input.validation.InputValidation
import components.input.InputError
import org.scalajs.dom.ext.KeyCode
import components.chip.ChipIconTrailing
import components.input.validation.InputValidationComponent
import org.scalajs.dom.raw.File
import components.input.InputComponent
import components.MDCChipSet

class InputChipSet(
    val textField: TextField[html.Input],
    initialChips: Set[Chip] = Set()
) extends BaseChipSet[Chip](initialChips)
    with MaterialInput[List[String], MDCChipSet, html.Input] {

  val chipSetIndex = Var(0)
  override val initialStyles: Seq[String] =
    baseStyle ++ Seq("mdc-chip-set--input")
  override def render(): HtmlElement = {
    div(textField, super.render())
  }

  def onEnterPressed = getInputElement
    .events(onKeyPress)
    .filter(_.keyCode == KeyCode.Enter)
    .map(_.preventDefault())
    .filter(_ => !textField.getValue().now().isEmpty())
    .mapTo({
      val textValue = textField.getValue().now()
      textField.clearInput()
      textValue
    })

  override def getInputElement(): ReactiveHtmlElement[html.Input] = textField
    .getInputElement()

  override def getValue(): Var[List[String]] = Var(
    chips
      .now()
      .map(_.valueVar.now())
      .toList
  )

  override def clearInput(): Unit = {
    chipSetIndex.set(0)
    chips.now().map(_.destroy())
    textField.clearInput()
  }

}
