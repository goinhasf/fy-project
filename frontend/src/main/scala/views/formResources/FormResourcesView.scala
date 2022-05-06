package views.formResources
import scala.reflect.ClassTag

import com.raquo.laminar.api.L._
import com.raquo.waypoint.Route
import com.raquo.waypoint.Router
import components.BaseUIComponents
import components.list.MaterialList
import components.list.item.MaterialListItem
import components.list.item.builders.AfterListItemIcon
import components.list.item.builders.BeforeListItemIcon
import components.list.item.builders.TwoLineListItem
import components.style.ComponentStyle
import dao.forms.FormResource
import dao.forms.FormResourceDetails

import urldsl.language.dummyErrorImpl._
import shared.pages.FormResourcesPage
import shared.pages.Page
import views.ViewImpl
import views.ViewImplWithNav
import components.dialog.Dialog
import components.id.ComponentID
import components.button.MaterialButton
import components.button.label.TextButtonLabel
import components.button.style.ButtonStyles
import components.form._
import org.scalajs.dom.experimental.HttpMethod
import org.scalajs.dom.document
import org.scalajs.dom.raw.FormData
import org.scalajs.dom.ext.Ajax
import dao.PrivateOwnership
import dao.forms.FormCategory
import components.form.Form
import components.form.JsonFormPart
import components.form.FileFormPart
import endpoints4s.Invalid
import com.raquo.airstream.ownership.OneTimeOwner
import components.input.InputError
import org.scalajs.dom.raw.File
import views.formResources.form.NewFormResourceDialog
import components.input.text._
import components.input.validation._
import components.input.text.content.FloatingLabel
import components.chip.FilterChipSet
import components.chip.Chip
import views.formResources.chipSet.CategoriesFilterChipSet
import components.list.FilteredList
import components.chip.ChipSelected
import providers.ContextProvider
import shared.pages.content.EmptyContent
import shared.pages.content.FormResourcesContent
import components.menu.MenuAnchoredComponent
import components.menu.ContextMenu
import components.menu.ContextMenuItem
import shared.pages.FormResourcePage
import components.DisplayFabEvent
import components.navigation.appbar.TopAppBar
import dao.users.UserInfo

class FormResourcesView(
    implicit val uiComponents: BaseUIComponents,
    override implicit val contextProvider: ContextProvider[FormResourcesContent]
) extends ViewImplWithNav[Unit, FormResourcesContent]() {

  override type PageT = FormResourcesPage
  override implicit val tag: ClassTag[PageT] = ClassTag(
    classOf[FormResourcesPage]
  )
  override def route(): Route[PageT, Unit] = Route.static(
    FormResourcesPage(),
    root / "form-resources" / endOfSegments
  )

  val categoryFilterChipSet = new CategoriesFilterChipSet()

  val formSearchBar               = new FilledTextField("Search")
  val errors: Var[Option[String]] = Var(None)
  override def renderContent(pageT: Signal[PageT])(implicit
      router: Router[Page]
  ): HtmlElement = {

    uiComponents.topAppBar.primaryButton.set(TopAppBar.MenuButton)

    div(
      cls := "content",
      child <-- contextProvider
        .getContext()
        .map(_.protectedInfo.userInfo)
        .map(renderList)
    )
  }

  def renderList(userInfo: UserInfo)(implicit router: Router[Page]) = {
    val newFormResourceDialog = NewFormResourceDialog(userInfo)

    uiComponents.topAppBar.title.set("Form Resources")
    uiComponents.eventBus.emit(DisplayFabEvent(true))
    uiComponents
      .fab
      .amend(onClick --> { _ =>
        newFormResourceDialog
          .mdcComponent
          .now()
          .map(_.open())
      })

    val formResources = FormResourcesList()
    val filteredList  = new FilteredList(formResources)
    val formResourcesObserver = Observer[Seq[FormResource]] { resources =>
      formResources
        .listItems
        .set(resources.toList.map(FormResourceListItem(_)))

      val categories = resources.flatMap(_.details.categories).toSet
      categoryFilterChipSet.setCategories(categories)

    }
    div(
      h1("Form Categories"),
      div(span("Categories"), categoryFilterChipSet),
      div(
        cls := "spanned",
        h2("Forms", styleAttr := "margin-right: 1rem"),
        formSearchBar,
        errors.now() match {
          case Some(value) => div(s"Errors: $value")
          case None        => emptyNode
        },
        onFormDialogResultReturned(formResources, newFormResourceDialog)
      ),
      filteredList,
      newFormResourceDialog,
      onCategoryFilterClicked(filteredList),
      setDrawerHeader,
      onMountBind(ctx =>
        context
          .signal
          .map(_.toSeq.flatMap(_.formResources)) --> formResourcesObserver
      )
    )
  }

  private def setDrawerHeader = contextProvider
    .getContext()
    .map(_.protectedInfo) --> (uiComponents
    .drawer
    .onUserInfoSetHeader(_))

  private def onCategoryFilterClicked(
      filteredList: FilteredList[FormResourceListItem]
  ): Binder[HtmlElement] =
    categoryFilterChipSet
      .chipsEventStream
      .collect({ case ChipSelected(_, _) =>
        categoryFilterChipSet
          .getSelected()
          .map(chip => FormCategory(chip.valueVar.now()))
          .map(category =>
            (item: FormResourceListItem) =>
              item.formResource.details.categories.contains(category)
          )
          .toSeq
      }) --> filteredList.filtersVar

  private def onFormDialogResultReturned(
      formResources: FormResourcesList,
      dialog: NewFormResourceDialog
  )(implicit router: Router[Page]): Binder[HtmlElement] = {

    def handleResult(result: Either[Invalid, FormResource]) = result match {
      case Left(invalid) =>
        errors
          .set(Some(invalid.errors.reduce((a, b) => s"$a\n$b")))
      case Right(result) => {
        // Add Form resource
        formResources
          .listItems
          .update(items => FormResourceListItem(result) +: items)

        // Add categories
        result
          .details
          .categories
          .foreach(categoryFilterChipSet.addCategory)

      }
    }

    dialog
      .form
      .eventBus
      .events
      .collect({ case FormResultReceived(result) => result }) --> handleResult _
  }

}

case class FormResourcesList(formResources: FormResource*)(implicit
    router: Router[Page]
) extends MaterialList[FormResourceListItem] {

  override val listItems: Var[Seq[FormResourceListItem]] = Var(
    formResources.map(e => FormResourceListItem(e)).toList
  )

}

case class FormResourceListItem(formResource: FormResource)(implicit
    router: Router[Page]
) extends MaterialListItem
    with TwoLineListItem
    with BeforeListItemIcon
    with AfterListItemIcon {

  override lazy val afterListItemIcon: Var[String]  = Var("create")
  override lazy val beforeListItemIcon: Var[String] = Var("favorite")

  override lazy val primaryTextVar: Var[String] = Var(
    formResource.details.name
  )
  override lazy val secondaryTextVar: Var[String] = Var {
    val text = formResource
      .details
      .categories
      .foldRight("")((a, b) => s"${a.categoryName}, $b")
    text.substring(0, text.length() - 2)
  }

  override def render(): HtmlElement =
    super
      .render()
      .amend(
        padding := "0.85rem",
        onClick --> { _ =>
          router.pushState(new FormResourcePage(formResource._id))
        }
      )

}
