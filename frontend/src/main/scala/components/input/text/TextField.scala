package components.input.text

import com.raquo.laminar.api.L._
import components.MaterialComponent
import components.id.Identifiable
import components.MDCTextField
import components.input.text.content._
import components.id.ComponentID
import components.style.ComponentStyle
import components.ripple.Ripple
import components.input.validation.{InputValidation, InputValidationComponent}
import components.input.text.HelperTextComponent
import components.input.MaterialInput
import org.scalajs.dom.html
import com.raquo.laminar.nodes.ReactiveHtmlElement
import components.input.text.styles.TextFieldStyles
import components.input.text.styles.OutlinedTextFieldStyle
import components.input.text.styles.FilledTextFieldStyle
import components.input.text.HelperText
import components.input.validation.NoValidation
import components.input.InputError
import org.scalajs.dom.document

trait TextField[+Ref <: html.Element]
    extends MaterialInput[String, MDCTextField, Ref]
    with HelperTextComponent
    with Identifiable
    with ComponentStyle
    with Ripple {

  val id: ComponentID = ComponentID("mdc-text-field")

  val textFieldContent: TextFieldContent[Ref]

  protected lazy val rootElement: HtmlElement =
    label(
      idAttr := id.toString(),
      cls <-- stylesMap.signal,
      ripple,
      textFieldContent.render(),
      onMountCallback(ctx => {
        mdcComponent.set({
          val component = new MDCTextField(ctx.thisNode.ref)
          component.useNativeValidation = false
          Some(component)
        })
      })
    )

  override def render(): HtmlElement = div(
    rootElement,
    helperText.render()
  )

  def setValid(): Unit = {
    helperText.clearError
    mdcComponent.update(_.map(c => {
      c.valid = true
      c
    }))
  }

  def setInvalid(err: String): Unit = {
    helperText.makeError(err)
    mdcComponent.update(_.map(c => {
      c.valid = false
      c
    }))
  }

  def floatLabel(toggle: Boolean = true) = textFieldContent
    .label
    .stylesMap
    .update(_ + ("mdc-floating-label--float-above" -> toggle))

  def getInputElement(): ReactiveHtmlElement[Ref] =
    textFieldContent.inputElement

  def editContent(f: TextFieldContent[Ref] => Unit): TextField[Ref] = {
    f(textFieldContent)
    this
  }

  def getValue(): Var[String] = textFieldContent.valueVar

  def clearInput(): Unit = textFieldContent.valueVar.set("")

}

class OutlinedTextField(
    label: String,
    override val helperText: HelperText = HelperText.NoHelperText
) extends TextField[html.Input]
    with OutlinedTextFieldStyle {

  override val textFieldContent = OutlinedTextFieldContent(
    FloatingLabel(id, label)
  )

  override def clearInput(): Unit = {
    super.clearInput()
    getInputElement().ref.value = ""
  }

}
class FilledTextField(
    label: String,
    override val helperText: HelperText = HelperText.NoHelperText
) extends TextField[html.Input]
    with FilledTextFieldStyle {
  override val textFieldContent = FilledTextFieldContent(
    FloatingLabel(id, label)
  )

  override def clearInput(): Unit = {
    super.clearInput()
    getInputElement().ref.value = ""
  }

}
