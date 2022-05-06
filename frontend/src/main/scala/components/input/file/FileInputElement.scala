package components.input.file

import com.raquo.laminar.api.L._
import components.input.text.HelperText
import components.input.text.OutlinedTextField
import components.input.validation.NoValidation
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html
import components.input.text.content.OutlinedTextFieldContent
import components.input.text.content.TextFieldContent
import components.input.text.content.FloatingLabel
import components.input.text.TextField
import components.input.validation.InputValidation
import components.input.text.styles.TextFieldStyles
import components.id.ComponentID
import org.scalajs.dom.raw.File
import components.input.InputError
import components.MaterialComponent
import components.MDCComponent
import components.id.Identifiable
import components.input.MaterialInput
import components.input.text.HelperTextComponent
import components.style.ComponentStyle
import components.ripple.Ripple
import components.input.validation.InputValidationComponent

class OutlinedFileInputElement(
    labelValue: String,
    helperText: String = ""
) extends MaterialComponent[MDCComponent]
    with Identifiable
    with ComponentStyle
    with Ripple
    with MaterialInput[Option[File], MDCComponent, html.Input] {

  val errorText                       = Var(helperText)
  val fileSelected: Var[Option[File]] = Var(None)

  override val id: ComponentID = ComponentID("mdc-file-input")

  override val initialStyles: Seq[String] = TextFieldStyles.outlinedStyle

  val inputFieldContent = new OutlinedTextFieldContent(
    FloatingLabel(id, labelValue)
  ) {

    val labelStyle                     = ComponentStyle(Seq("mdc-file-input-label"))
    override val valueVar: Var[String] = Var(label.text)
    override lazy val inputElement: ReactiveHtmlElement[html.Input] = input(
      cls := "mdc-text-field__input mdc-file-input",
      typ := "file",
      ariaLabeledBy := label.id.toString(),
      inContext(ctx =>
        onInput.mapTo(ctx.ref.files(0)) --> { file =>
          val maybeFile = Option.when(!scalajs.js.isUndefined(file))(file)
          fileSelected.set(maybeFile)
          maybeFile.map(file => valueVar.set(file.name))
        }
      )
    )

    override def render(): Mod[HtmlElement] = Seq(
      span(
        cls := "mdc-notched-outline",
        span(
          cls := "mdc-notched-outline__leading",
          display := "flex",
          alignItems := "center",
          i(cls := "material-icons md-dark", marginLeft := "0.5rem", "attach_file")
        ),
        span(cls := "mdc-notched-outline__notch"),
        span(cls := "mdc-notched-outline__trailing")
      ),
      span(
        cls <-- labelStyle.stylesMap.signal,
        child.text <-- valueVar.signal
      ),
      inputElement
    )
  }

  protected lazy val rootElement: HtmlElement = label(
    idAttr := id.toString(),
    cls <-- stylesMap.signal,
    ripple,
    inputFieldContent.render()
  )

  override def render(): HtmlElement = div(
    rootElement,
    div(
      cls := "mdc-file-input-label--invalid mdc-file-input-error-label",
      child.text <-- errorText.signal
    )
  )

  override def getInputElement(): ReactiveHtmlElement[html.Input] =
    inputFieldContent.inputElement

  override def getValue(): Var[Option[File]] = fileSelected

  def setValid(): Unit = {
    stylesMap.update(_ - "mdc-text-field--invalid")
    errorText.set("")
    inputFieldContent
      .labelStyle
      .stylesMap
      .update(_ - "mdc-file-input-label--invalid")
  }
  def setInvalid(err: String): Unit = {
    stylesMap.update(_ + ("mdc-text-field--invalid" -> true))
    errorText.set(err)
    inputFieldContent
      .labelStyle
      .stylesMap
      .update(_ + ("mdc-file-input-label--invalid" -> true))

  }

  def clearInput(): Unit = {
    getInputElement().ref.value = ""
    inputFieldContent.valueVar.set(labelValue)
    fileSelected.set(None)
  }
}
