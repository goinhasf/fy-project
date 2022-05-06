package views.selectSociety

import components.BaseUIComponents
import providers.ContextProvider
import shared.pages.content.ProtectedPageContent
import views.ViewImpl
import shared.pages.SelectSocietyPage
import scala.reflect.ClassTag
import com.raquo.laminar.api.L._
import com.raquo.waypoint.Router
import components.DisplayFabEvent
import components.navigation.appbar.TopAppBar
import shared.pages.LoginPage
import com.raquo.waypoint.Route
import urldsl.language.dummyErrorImpl._
import shared.pages.Page
import views.ViewImplWithNav
import components.list.MaterialList
import dao.societies.Society
import components.list.item.MaterialListItem
import components.list.item.builders.EmptyBeforeListElement
import components.list.item.builders.EmptyAfterListElement
import components.list.item.builders.SingleLineTextElement
import shared.pages.content.SelectSocietyContent
import services.PageEndpoints
import org.scalajs.dom.document
import shared.pages.SelectSocietyContext
import components.button.MaterialButton
import components.button.label.TextButtonLabel
import components.button.style.ButtonStyles
import shared.pages.SocietyRegistrationPage

class SelectSocietyView(
    implicit val uiComponents: BaseUIComponents,
    override implicit val contextProvider: ContextProvider[SelectSocietyContent]
) extends ViewImplWithNav[Unit, SelectSocietyContent]() {
  override type PageT = SelectSocietyPage
  override implicit val tag: ClassTag[PageT] = ClassTag(
    classOf[SelectSocietyPage]
  )

  override def renderContent(
      pageT: Signal[PageT]
  )(implicit
      router: Router[Page]
  ): HtmlElement = {
    uiComponents.eventBus.emit(DisplayFabEvent(false))
    uiComponents.topAppBar.primaryButton.set(TopAppBar.BackButton)
    uiComponents.topAppBar.title.set("Society selection")

    val societiesList = SocietiesList(Seq())

    div(
      cls := "content",
      h1("Select the society you want to manage"),
      societiesList,
      MaterialButton(
        TextButtonLabel("Register New Society"),
        ButtonStyles.outlinedButtonStyle
      )
        .amend(
          onClick.mapTo(SocietyRegistrationPage()) --> (router.pushState(_))
        )
    ).amend(
      context.signal --> { context =>
        createListOnContextReceived(societiesList, context)
      },
      uiComponents.topAppBar.onBackButtonClicked --> { _ =>
        router.pushState(LoginPage())
      }
    )
  }

  private def createListOnContextReceived(
      list: SocietiesList,
      context: Option[SelectSocietyContent]
  ) =
    context match {
      case Some(value) => {
        val items = value
          .userSocieties
          .toSeq
          .map(SocietyListItem)
          .map(
            _.editRoot(el =>
              onMountBind(ctx => onListItemClicked(value, el, ctx))
            )
          )
        list.listItems.set(items)
      }
      case None =>
    }

  private def onListItemClicked(
      pageContext: SelectSocietyContent,
      el: SocietyListItem,
      ctx: MountContext[HtmlElement]
  ) =
    onClick.mapTo(el.society._id) --> { id =>
      val firstName = pageContext.protectedInfo.userInfo.firstName
      val lastName  = pageContext.protectedInfo.userInfo.lastName
      PageEndpoints
        .submitSocietySelection(id)
        .foreach(_ => document.location.href = "/")(ctx.owner)
    }

  override def route(): Route[PageT, Unit] = Route(
    encode = page => (),
    decode = _ => SelectSocietyPage(),
    pattern = root / "select-society" / endOfSegments
  )

}

case class SocietyListItem(society: Society)
    extends MaterialListItem
    with EmptyBeforeListElement
    with EmptyAfterListElement
    with SingleLineTextElement {
  override val textVar: Var[String] = Var(society.details.name)
}

case class SocietiesList(items: Seq[SocietyListItem])
    extends MaterialList(items)
