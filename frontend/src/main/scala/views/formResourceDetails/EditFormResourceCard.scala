package views.formResourceDetails

import components.card.Card
import com.raquo.laminar.api.L
import components.editable.EditableField
import components.input.chip.InputChipSet
import components.chip.BaseChipSet
import components.chip.Chip
import components.input.text.FilledTextField
import components.MDCChipSet
import org.scalajs.dom.html
import com.raquo.laminar.api.L._
import dao.forms.FormResource
import components.card.ActionableCard
import components.card.CardActionButton
import components.input.text.OutlinedTextField
import components.chip.ChipIconTrailing
import services.FormResourceService
import dao.forms.FormCategory
import dao.forms.FormResourceDetails
import dao.users.UserInfo
import shared.auth.RegularUserRole

class EditFormResourceCard(formResource: FormResource, userInfo: UserInfo)
    extends ActionableCard(
      s"Form Resource Details",
      "Here you can edit the details of your resource",
      None,
      List()
    ) {
  
    def editable = {
      display {
      if (formResource.details.isPublic) {
        if (userInfo.role.roleType != RegularUserRole()) {
          "flex"
        } else {
          "none"
        }
      } else {
        "flex"
      }
    }
    }
  val isEditingVar    = Var(false)
  val formResourceVar = Var(formResource)
  val editButton = CardActionButton("Edit")
    .editRoot(el =>
      isEditingVar.signal.map {
        _ match {
          case true  => "Update"
          case false => "Edit"
        }

      } --> (newLabel => el.buttonLabel.label.set(Some(newLabel)))
    )
    .editRoot(editable)

  private def categoryChips(categories: List[FormCategory]) =
    categories
      .map(_.categoryName)
      .map(name =>
        new Chip(
          name,
          trailingIcon = Some(
            new ChipIconTrailing().editRoot(
              display <-- isEditingVar.signal.map(!_).map {
                case true  => "none"
                case false => "block"
              }
            )
          )
        ).editRoot(pointerEvents <-- isEditingVar.signal.map(!_).map {
          case true  => "none"
          case false => "auto"
        })
      )
      .toSet

  val nameField = new FilledTextField("Name")
    .editRoot { el =>
      el.textFieldContent.valueVar.set(formResource.details.name)
      formResourceVar.signal.map(_.details.name) --> el
        .textFieldContent
        .valueVar
      el.floatLabel()
      el.fromFoundation(_.disabled = true)
      el.textFieldContent
        .inputElement
        .amend(disabled <-- isEditingVar.signal.map(!_))
      isEditingVar.signal.map(!_) --> { disabled =>
        el.fromFoundation(_.disabled = disabled)
      }

    }
  val notesField = new FilledTextField("Notes")
    .editRoot { el =>
      el.textFieldContent.valueVar.set(formResource.details.notes)
      formResourceVar.signal.map(_.details.notes) --> el
        .textFieldContent
        .valueVar
      el.floatLabel()
      el.fromFoundation(_.disabled = true)
      el.textFieldContent
        .inputElement
        .amend(disabled <-- isEditingVar.signal.map(!_))
      isEditingVar.signal.map(!_) --> { disabled =>
        el.fromFoundation(_.disabled = disabled)
      }

    }
  val categoriesField = new InputChipSet(
    new FilledTextField("Category").editRoot(el => {
      el.fromFoundation(_.disabled = true)
      el.floatLabel()
      el.textFieldContent
        .inputElement
        .amend(disabled <-- isEditingVar.signal.map(!_))
      isEditingVar.signal.map(!_) --> { disabled =>
        el.fromFoundation(_.disabled = disabled)
      }
    }),
    categoryChips(formResource.details.categories)
  ).editRoot(el =>
    el.onEnterPressed --> { name =>
      el.addChip(
        new Chip(
          name,
          trailingIcon = Some(
            new ChipIconTrailing().editRoot(
              display <-- isEditingVar.signal.map(!_).map {
                case true  => "none"
                case false => "block"
              }
            )
          )
        ).editRoot(pointerEvents <-- isEditingVar.signal.map(!_).map {
          case true  => "none"
          case false => "auto"
        })
      )
    }
  ).editRoot(el =>
    formResourceVar
      .signal
      .map(_.details.categories)
      .map(categoryChips) --> el.chips
  )
  private def fields = Seq(nameField, notesField, categoriesField)

  def onUpdateClicked = isEditingVar
    .signal
    .changes
    .filter(_ == false)
    .map(_ =>
      FormResourceDetails(
        nameField.getValue().now(),
        notesField.getValue().now(),
        categoriesField.getValue().now().map(FormCategory(_)),
        userInfo.role.roleType != RegularUserRole(),
        formResource.details.fileId
      )
    )

  def onEditActionClicked = editButton
    .events(onClick.mapTo(!isEditingVar.now()))

  override val cardContent: Option[L.HtmlElement] = Some(
    div(
      cls := "edit-grid",
      fields.map(el => div(cls := "editable-field", el.render())),
      onEditActionClicked --> isEditingVar,
      onUpdateClicked
        .map(resource =>
          FormResourceService.updateFormResource(formResource._id, resource)
        )
        .flatten
        .collect({ case Some(resource) => resource }) --> formResourceVar
    )
  )

  override val actions: Var[List[CardActionButton]] = Var(
    List(editButton)
  )
}
