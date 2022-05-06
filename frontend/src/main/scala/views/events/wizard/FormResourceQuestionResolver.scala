package views.events.wizard

import com.raquo.laminar.api.L._
import components.button.MaterialButton
import components.button.label.TextButtonLabel
import components.button.style.ButtonStyles
import dao.events.FormResourcesQuestionResolver
import dao.forms.FormResource
import io.circe.JsonObject
import io.circe.syntax._
import org.scalajs.dom.document
import services.FormResourceService
import views.formResourceDetails.FormResourceFieldComponent

import Utils._
import com.github.uosis.laminar.webcomponents.material
import com.raquo.waypoint.Router
import shared.pages.Page
import shared.pages.EventWizardQuestionFormPage
import com.github.uosis.laminar.webcomponents.material.IconButton
import dao.events.EventWizardQuestionResolver
import shared.endpoints.events.wizard.ops.GetEventWizardQuestionState

object FormResourceQuestionResolver {

  def apply(
      wizardId: String,
      wizardStateId: String,
      resolver: FormResourcesQuestionResolver,
      eventWizardQuestionState: GetEventWizardQuestionState
  )(implicit nextPageBus: EventBus[Page]): HtmlElement = {

    def listItemFactory(formResource: FormResource) = material
      .List
      .ListItem(
        _.twoline(true),
        _.slots.default(span(formResource.details.name)),
        _.slots.secondary(span(formResource.details.notes))
      )
      .amend(
        onClick.mapTo(
          EventWizardQuestionFormPage(
            wizardId,
            wizardStateId,
            eventWizardQuestionState.question._id,
            formResource._id
          )
        ) --> nextPageBus
      )

    def renderListOfResources(resources: Seq[FormResource]) =
      resources.foldRight(material.List())((resource, list) =>
        list.amend(material.List.slots.default(listItemFactory(resource)))
      )

    val getFormsInState =
      EventStream
        .combineSeq(
          eventWizardQuestionState
            .state
            .flatMap(_.data)
            .toSeq
            .flatMap { obj =>
              val formsObj = adjustCursor(Seq("forms"), obj)
              val keys     = formsObj.keys.toSeq
              keys
                .map(FormResourceService.getResource)
                .map(_.collect({ case Some(value) => value }))
            }
        )
        .startWith(Seq())

    val formResourcesStream = EventStream.combineSeq(
      resolver
        .formIds
        .keys
        .toSeq
        .map(FormResourceService.getResource)
        .map(_.collect({ case Some(value) => value }))
    )

    div(
      h2("Suggested Forms", marginBottom("1rem")),
      div(child <-- formResourcesStream.map(renderListOfResources)),
      div(
        display <-- getFormsInState.map(seq =>
          Option.when(seq.size > 0)("grid").getOrElse("none")
        ),
        cls("form-header"),
        h2("Form Attachments"),
        IconButton(_.icon("add"), _.label("Add your own form"))
      ),
      div(child <-- getFormsInState.map(renderListOfResources))
    )

  }

}
