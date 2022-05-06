package views.ui

import components.navigation.drawer.NavigationDrawer
import components.list.item.MaterialListItem
import components.list.item.builders.SingleLineTextElement
import components.list.item.builders.EmptyAfterListElement
import components.list.item.builders.BeforeListItemIcon
import com.raquo.laminar.api.L._
import shared.pages.FormResourcesPage
import com.raquo.waypoint.Router
import shared.pages.Page
import shared.pages.RootPage
import components.list.MaterialList
import components.navigation.drawer.NavigationDrawerList
import components.navigation.drawer.NavigationDrawerItem
import dao.users.UserInfo
import components.navigation.drawer.NavigationDrawerHeader
import components.button.MaterialButton
import components.button.label.TextButtonLabel
import components.button.style.ButtonStyles
import com.raquo.laminar.nodes.ReactiveHtmlElement
import shared.pages.SocietyRegistrationPage
import shared.pages.content.ProtectedPageContent
import shared.pages.SelectSocietyPage
import shared.pages.EventsPage
import org.scalajs.dom
import shared.auth.GuildAdminRole
import shared.auth.AdminRole
import shared.pages.AdminMainPage
import shared.pages.AdminEventSubmissionsPage

sealed trait AppNavigationDrawerEvent
case class PageNavigationClickEvent[T <: Page](page: T)
    extends AppNavigationDrawerEvent

class AppNavigationDrawer extends NavigationDrawer {
  val navigationEventBus = new EventBus[AppNavigationDrawerEvent]
  override val materialList: NavigationDrawerList =
    new NavigationDrawerList(Seq()).fromFoundation { comp =>
      comp.singleSelection = true
      comp.initializeListType()
    }

  override def contentElement: HtmlElement = super
    .contentElement
    .amend(cls := "app-drawer__content")

  def onPageChange = navigationEventBus
    .events
    .collect({ case PageNavigationClickEvent(page) => page })

  def isUserAdmin(userInfo: UserInfo) = {
    userInfo.role.roleType == AdminRole() || userInfo
      .role
      .roleType == GuildAdminRole()
  }

  def onUserInfoSetHeader(content: ProtectedPageContent) = drawerContentVar
    .update(map =>
      Map[String, HtmlElement](
        "header" -> new NavigationDrawerHeader(
          s"${content.userInfo.firstName} ${content.userInfo.lastName}", {
            if (isUserAdmin(content.userInfo)) {
              "Admin Panel"
            } else {
              content
                .society
                .map(_.details.name)
                .getOrElse("Error: society not set")
            }

          }
        )
      ) ++ Map[String, HtmlElement](
        "list" -> materialList.editRoot { root =>
          if (isUserAdmin(content.userInfo)) {
            root.listItems.set(AppNavigationDrawer.adminItems(this))
          } else {
            root.listItems.set(AppNavigationDrawer.items(this))
          }
          emptyMod
        }
      ) + ("accountActions" ->
        div(
          display := "grid",
          MaterialButton(
            TextButtonLabel("Switch Society"),
            ButtonStyles.outlinedButtonStyle
          ).editRoot(
            onClick --> { _ => mdcComponent.now().map(_.open = false) },
            cls := "mdc-drawer-content--bottom-actions"
          ).editRoot(
            onClick.mapTo(
              PageNavigationClickEvent(SelectSocietyPage())
            ) --> navigationEventBus
          ).editRoot(display := {
            if (isUserAdmin(content.userInfo)) {
              "none"
            } else {
              "block"
            }
          }),
          MaterialButton(
            TextButtonLabel("Logout"),
            ButtonStyles.outlinedButtonStyle
          ).editRoot(
            cls := "mdc-drawer-content--bottom-actions",
            color := "var(--mdc-theme-error)"
          )
        ))
    )
}
object AppNavigationDrawer {

  def adminItems(implicit appDrawer: AppNavigationDrawer) = Seq(
    new NavigationDrawerItem()
      with SingleLineTextElement
      with BeforeListItemIcon
      with EmptyAfterListElement {
      val beforeListItemIcon: Var[String] = Var("description")
      val textVar: Var[String]            = Var("Event Submissions")
    }.editRoot(el =>
      el.amend(
        onClick.mapTo(
          PageNavigationClickEvent(AdminEventSubmissionsPage())
        ) --> appDrawer.navigationEventBus
      )
    ),
    new NavigationDrawerItem()
      with SingleLineTextElement
      with BeforeListItemIcon
      with EmptyAfterListElement {
      val beforeListItemIcon: Var[String] = Var("grading")
      val textVar: Var[String]            = Var("Form Submissions")
    }.editRoot(el =>
      el.amend(
        onClick.mapTo(
          PageNavigationClickEvent(AdminMainPage())
        ) --> appDrawer.navigationEventBus
      )
    ),
    new NavigationDrawerItem()
      with SingleLineTextElement
      with BeforeListItemIcon
      with EmptyAfterListElement {
      val beforeListItemIcon: Var[String] = Var("outbox")
      val textVar: Var[String]            = Var("Form Resources")
    }.editRoot(el =>
      el.amend(
        onClick.mapTo(
          PageNavigationClickEvent(FormResourcesPage())
        ) --> appDrawer.navigationEventBus
      )
    )
  )

  def items(implicit appDrawer: AppNavigationDrawer) = Seq(
    new NavigationDrawerItem()
      with SingleLineTextElement
      with BeforeListItemIcon
      with EmptyAfterListElement {
      val beforeListItemIcon: Var[String] = Var("outbox")
      val textVar: Var[String]            = Var("Form Resources")
    }.editRoot(el =>
      el.amend(
        onClick.mapTo(
          PageNavigationClickEvent(FormResourcesPage())
        ) --> appDrawer.navigationEventBus
      )
    ),
    new NavigationDrawerItem()
      with SingleLineTextElement
      with BeforeListItemIcon
      with EmptyAfterListElement {
      val beforeListItemIcon: Var[String] = Var("inbox")
      val textVar: Var[String]            = Var("Society Center")
    }.editRoot(el =>
      el.amend(
        onClick.mapTo(
          PageNavigationClickEvent(RootPage())
        ) --> appDrawer.navigationEventBus
      )
    ),
    new NavigationDrawerItem()
      with SingleLineTextElement
      with BeforeListItemIcon
      with EmptyAfterListElement {
      val beforeListItemIcon: Var[String] = Var("event")
      val textVar: Var[String]            = Var("Events")
    }.editRoot(el =>
      el.amend(
        onClick.mapTo(
          PageNavigationClickEvent(EventsPage())
        ) --> appDrawer.navigationEventBus
      )
    )
  )
}
