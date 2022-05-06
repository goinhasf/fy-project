package views.formResourceDetails

import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.util.Failure
import scala.util.Success

import services.FormResourceService
import com.raquo.laminar.api.L._
import com.raquo.waypoint.Renderer
import com.raquo.waypoint.Route
import com.raquo.waypoint.Router
import components.BaseUIComponents
import components.input._
import dao.forms.FormResource
import dao.forms.FormResourceDetails
import org.scalajs.dom

import urldsl.language.dummyErrorImpl._
import shared.pages.FormResourcePage
import shared.pages.Page
import shared.pages.RootPage
import views.ViewImpl
import views.ViewImplWithNav
import providers.ContextProvider
import shared.pages.content.EmptyContent
import components.chip.BaseChipSet
import shared.pages.FormResourcesPage
import dao.forms.FormCategory
import components.chip.Chip
import components.input.text.OutlinedTextField
import components.editable.EditableField
import components.card.ActionableCard
import components.navigation.appbar.TopAppBar
import components.navigation.appbar.PrimaryButtonClicked
import components.navigation.appbar.MenuButton
import components.navigation.appbar.BackButton
import io.circe.JsonObject
import dao.forms.FormResourceFieldDescriptor
import dao.forms.FormResourceFields
import shared.pages.content.ProtectedPageContent

class FormResourcePageImpl(
    implicit val uiComponents: BaseUIComponents,
    override implicit val contextProvider: ContextProvider[ProtectedPageContent]
) extends ViewImplWithNav[String, ProtectedPageContent]() {

  override type PageT = FormResourcePage
  override implicit val tag: ClassTag[PageT] = ClassTag(
    classOf[FormResourcePage]
  )

  override def route(): Route[PageT, String] = Route(
    encode = page => page.args,
    decode = args => FormResourcePage(args),
    root / "form-resources" / segment[String] / endOfSegments
  )

  val formResourceBus = new EventBus[FormResource]

  override def renderContent(pageT: Signal[PageT])(implicit
      router: Router[Page]
  ): HtmlElement = {

    uiComponents.fab.hide()
    uiComponents.topAppBar.primaryButton.set(TopAppBar.BackButton)

    div(
      cls := "content",
      child <-- formResourceBus
        .events
        .flatMap(resource =>
          contextProvider
            .getContext()
            .map(_.userInfo)
            .map(user => (resource, user))
        )
        .map(args =>
          div(
            new EditFormResourceCard(args._1, args._2),
            new EditFormResourceFieldsCard(args._1)
          )
        ),
      formResourceBus
        .events
        .map(resource => s"Form Resource #${resource._id}") -->
        uiComponents.topAppBar.title,
      createFormResourceStream(pageT, router),
      uiComponents
        .topAppBar
        .onBackButtonClicked --> { _ =>
        router.pushState(FormResourcesPage())
      }
    )
  }

  def createFormResourceStream(
      pageT: Signal[FormResourcePage],
      router: Router[Page]
  ) =
    pageT
      .flatMap(p =>
        FormResourceService
          .getResource(p.args)
      )
      .map {
        _ match {
          case Some(resource) => resource
          case _ => {
            router.pushState(FormResourcesPage())
            throw new Exception("Resource not found!")
          }
        }
      } --> formResourceBus

}
