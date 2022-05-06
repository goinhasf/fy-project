package components.dialog

import com.raquo.laminar.api.L._
import components.MDCDialog
import components.MaterialComponent
import components.id.Identifiable
import components.id.ComponentID
import components.MDCComponent

trait Dialog extends MaterialComponent[MDCDialog] with Identifiable {

  val id: ComponentID = ComponentID("mdc-dialog")
  val title: String
  val contentDescription: String
  val content: HtmlElement

  override protected lazy val rootElement: HtmlElement = {
    div(
      cls := "mdc-dialog",
      idAttr := id.toString(),
      div(
        cls := "mdc-dialog__container",
        div(
          cls := "mdc-dialog__surface",
          role := "alertdialog",
          ariaModal := "true",
          ariaLabeledBy := "dialog-title",
          ariaDescribedBy := content.ref.id.toString(),
          h2(idAttr := "dialog-title", cls := "mdc-dialog__title", title),
          div(
            cls := "mdc-dialog__content",
            idAttr := id.toString(),
            content
          )
        )
      ),
      div(cls := "mdc-dialog__scrim"),
      onMountCallback(ctx =>
        mdcComponent.set({
          val dialog = new MDCDialog(ctx.thisNode.ref)
          Some(dialog)
        })
      )
    )
  }
}
