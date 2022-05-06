package views.formResources.form

import components.dialog.Dialog
import com.raquo.laminar.api.L._
import endpoints4s.Invalid
import dao.forms.FormResource
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import components.MaterialComponent
import components.MDCComponent
import components.id.Identifiable
import components.form.FormResultReceived
import dao.users.UserInfo
import shared.auth.GuildAdminRole
import shared.auth.RegularUserRole

case class NewFormResourceDialog(userInfo: UserInfo) extends Dialog {

  override val title: String = "Upload a new form resource"

  override val contentDescription: String = "Form resource fields"

  val result = new EventBus[Either[Invalid, FormResource]]()

  val observer = new Observer[Either[Invalid, FormResource]] {

    def onNext(nextValue: Either[Invalid, FormResource]): Unit = {
      result.emit(nextValue)
      mdcComponent.now().map(_.close("Form resource added"))
      //form.formFields.map(_.component.clearInput())
    }

    def onError(err: Throwable): Unit = {
      form.errorVar.set(err.getMessage())
      println(err.getMessage())
    }

    def onTry(nextValue: Try[Either[Invalid, FormResource]]): Unit =
      nextValue match {
        case Failure(exception) => onError(exception)
        case Success(value)     => onNext(value)
      }
  }

  override def render(): HtmlElement = super
    .render()
    .amend(
      form
        .eventBus
        .events
        .collect({ case FormResultReceived(result) => result }) --> {
        _ match {
          case Left(err) => {}
          case Right(value) => {
            mdcComponent.now().map(_.close("Form resource added"))
            // form.formFields.map(_.component.clearInput())
          }
        }
      }
    )

  val form: NewFormResourceForm = NewFormResourceForm(userInfo.role.roleType != RegularUserRole())

  override val content: HtmlElement = form
}
