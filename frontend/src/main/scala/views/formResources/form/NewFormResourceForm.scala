package views.formResources.form

import services.FormResourceService
import com.raquo.laminar.api.L._
import components.button.MaterialButton
import components.button.label.TextButtonLabel
import components.button.style.ButtonStyles
import components.form.FormField
import components.form.MaterialForm
import io.circe.syntax._
import components.input.text.OutlinedTextField
import dao.PrivateOwnership
import dao.forms.FormCategory
import dao.forms.FormResource
import dao.forms.FormResourceDetails
import endpoints4s.Invalid
import org.scalajs.dom.experimental.HttpMethod
import components.form.FileFormPart
import components.form.Form
import components.form.JsonFormPart
import components.input.text.HelperText
import components.input.validation._
import components.input.InputError
import components.input.file.OutlinedFileInputElement
import org.scalajs.dom.html
import org.scalajs.dom.raw.File
import components.form.chip.InputChipFormField
import components.form.text.TextFormField
import components.form.file.FileInputFormField
import components.input.text.TextField
import components.MaterialComponent
import components.MDCComponent
import components.input.InputComponent
import components.input.MaterialInput
import components.input.chip.InputChipSet
import components.input.switch.Switch
import components.chip.Chip
import components.chip.ChipIconTrailing

case class NewFormResourceForm(isAdmin: Boolean)
    extends MaterialForm[Either[Invalid, FormResource]] {

  override val submitMethod: HttpMethod = HttpMethod.POST
  override val encodingType: String     = "multipart/form-data"

  override val submitElement: HtmlElement = div(
    cls := "mdc-dialog__actions",
    MaterialButton(
      TextButtonLabel("Submit"),
      ButtonStyles.dialogButtonStyle
    )
  )

  val nameField = new TextFormField(
    "name",
    new OutlinedTextField(
      "Name",
      new HelperText("Name of the form required")
    ).editRoot(display := "flex"),
    Required(_.isEmpty())
  )

  val fileField = new FileInputFormField(
    "file",
    new OutlinedFileInputElement("File"),
    Required(_.isEmpty)
  )

  val categoriesField = new InputChipFormField(
    "categories",
    new InputChipSet(
      new OutlinedTextField(
        "Categories",
        new HelperText("Press enter to add")
      ).editRoot(width := "100%")
    ).editRoot(el =>
      el.onEnterPressed --> { name =>
        el.addChip(
          new Chip(name, trailingIcon = Some(new ChipIconTrailing()))
        )
      }
    )
  )

  val notesField = new TextFormField(
    "notes",
    new OutlinedTextField(
      "Notes"
    ).editRoot(display := "flex")
  )

  val makePublicField = new FormField(
    "isPublic",
    new Switch(
      "Make Public"
    )
  ).editRoot(if (isAdmin) display.block else display.none)

  override val formFields = Seq(
    nameField,
    fileField,
    categoriesField,
    notesField,
    makePublicField
  ).map(_.render)

  override def toForm(): Either[InputError, Form] = {

    for {
      name           <- nameField.validateTransform()
      categoriesList <- categoriesField.validateTransform()
      fileOpt        <- fileField.validateTransform()
      file           <- fileOpt.toRight(InputError("File is required"))
      notes          <- notesField.validateTransform()
      isPublic       <- makePublicField.validateTransform()
      categories = categoriesList.map(FormCategory(_))
      formDetails = FormResourceDetails(
        name,
        notes,
        categories,
        isPublic
      )
      jsonFormPart = JsonFormPart("details", formDetails.asJson)
    } yield (Form(
      jsonFormPart,
      FileFormPart("file", file)
    ))

  }

  override def submit(form: Form): EventStream[Either[Invalid, FormResource]] =
    FormResourceService.insertResource(
      form
    )
}
