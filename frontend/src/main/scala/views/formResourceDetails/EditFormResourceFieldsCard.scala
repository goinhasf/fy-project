package views.formResourceDetails

import components.card.ActionableCard
import components.card.CardActionButton
import com.raquo.laminar.api.L._
import io.circe.Json
import io.circe.JsonObject
import components.input.text.OutlinedTextField
import components.form.FormField
import components.form.MaterialForm
import components.form.FormEvents
import org.scalajs.dom.experimental.HttpMethod
import components.form.Form
import components.input.InputError
import components.form.Form
import components.button.MaterialButton
import components.button.style.ButtonStyles
import components.button.label.TextButtonLabel
import components.form.JsonFormPart

import io.circe.syntax._
import io.circe._
import components.editable.EditableField
import components.MDCTextField
import org.scalajs.dom
import components.input.text.FilledTextField
import scala.scalajs.js.JSON
import services.FormResourceService
import dao.forms.FormResource
import dao.forms.FormResourceDetails
import dao.forms.FormResourceFieldDescriptor
import dao.forms.FormResourceFields
import dao.forms.SingleFormResourceFieldDescriptor
import dao.forms.LoopFormResourceFieldDescriptor

class EditFormResourceFieldsCard(
    resource: FormResource
) extends ActionableCard(
      s"Form Resource Fields",
      "Here you can edit the fields in the form",
      None,
      List()
    ) {

  val isEditingVar: Var[Boolean] = Var(false)

  val editButton = CardActionButton("Edit")
    .editRoot(el =>
      isEditingVar.signal.map {
        _ match {
          case true  => "Update"
          case false => "Edit"
        }
      } --> (newLabel => el.buttonLabel.label.set(Some(newLabel)))
    )
    .editRoot(
      onClick --> { _ => isEditingVar.update(!_) }
    )
  val previewButton = CardActionButton("Preview").editRoot(
    onClick.mapTo(resource._id) --> { id =>
      dom.document.location.href = FormResourceService
        .getFormResourceAsPdf(
          id,
          Some(FormResourceFieldComponent.getJsonObjectFromFields(inputs))
        )
    },
    visibility <-- isEditingVar.signal.map {
      _ match {
        case true  => "hidden"
        case false => "visible"
      }
    }
  )

  lazy val inputs = FormResourceFieldComponent
    .createInputsFromJsonObject(
      resource.fields,
      resource.defaultFieldValues
    )
    .map(
      _.editRoot(padding := "0.5rem")
        .editRoot(ctx => isEditingVar --> ctx.isEditingVar)
    )

  override val cardContent: Option[HtmlElement] = Some(
    div(
      cls := "edit-grid",
      inputs,
      onSubmitClicked.mapTo(
        FormResourceFieldComponent.getJsonObjectFromFields(inputs)
      ) --> { json =>
        FormResourceService.updateFormAnswers(
          resource._id,
          json
        )
      }
    )
  )

  def onSubmitClicked = editButton.events(
    onClick
      .mapTo(isEditingVar.now())
      .filter(_ == false)
  )

  override val actions: Var[List[CardActionButton]] = Var(
    List(editButton, previewButton)
  )
}
