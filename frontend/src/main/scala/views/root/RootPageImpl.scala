package views.root
import com.raquo.laminar.api.L._
import com.raquo.waypoint.Route
import shared.pages.RootPage
import urldsl.language.dummyErrorImpl._

import com.raquo.waypoint.Router
import shared.pages.Page
import shared.pages.FormResourcePage
import scala.reflect.ClassTag
import components.BaseUIComponents
import components.button.MaterialButton
import org.scalajs.dom
import components.input.text.OutlinedTextField
import components.input.validation.NoValidation
import components.input.text.HelperText
import components.input.InputError
import components.input.text.content.FloatingLabel
import components.input.chip.InputChipSet
import components.chip.Chip
import org.scalajs.dom.ext.KeyCode
import components.chip.ChipIconTrailing
import views.ViewImplWithNav
import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import providers.ContextProvider
import shared.pages.content.RootPageContent
import components.DisplayFabEvent
import components.navigation.appbar.TopAppBar
import dao.societies.Society
import components.card.Card
import services.FileServiceClient
import dao.societies.SocietyDetails
import com.github.uosis.laminar.webcomponents.material.Textarea
import com.github.uosis.laminar.webcomponents.material.Textfield
import com.github.uosis.laminar.webcomponents.material
import components.id.ComponentID

class RootPageImpl(
    implicit val uiComponents: BaseUIComponents,
    override implicit val contextProvider: ContextProvider[RootPageContent]
) extends ViewImplWithNav[Unit, RootPageContent]() {
  val isEditing = Var(false)

  override type PageT = RootPage
  override implicit val tag: ClassTag[PageT] = ClassTag(classOf[RootPage])
  override def renderContent(
      pageT: Signal[PageT]
  )(implicit
      router: Router[Page]
  ): HtmlElement = {
    uiComponents.eventBus.emit(DisplayFabEvent(false))
    uiComponents.topAppBar.primaryButton.set(TopAppBar.MenuButton)
    uiComponents.topAppBar.title.set("Society Centre")

    div(
      cls := "content",
      child <-- renderSocietyInfo(
        contextProvider
          .getContext()
          .map(Some(_))
          .toSignal(None)
          .map(_.flatMap(_.protectedInfo.society))
      ),
      setDrawerHeader
    )
  }

  private def renderSocietyInfo(
      signal: Signal[Option[Society]]
  ): Signal[HtmlElement] = {

    signal.map {
      case Some(society) => {
        val details: SocietyDetails = society.details
        val editButton = material
          .Button(
            _.label <-- isEditing
              .signal
              .map({
                case true  => "Update"
                case false => "Edit"
              })
          )
          .amend(onClick.mapTo(!isEditing.now()) --> isEditing)

        val publicEmailAddress = Textfield(
          _.value(details.publicEmailAddress.getOrElse("")),
          _.label("Society's email address"),
          _.disabled <-- isEditing.signal.map(!_)
        ).amend(padding := "1rem")

        val webPageUrl = Textfield(
          _.label("Web Page Url"),
          _.value(details.webPageUrl.getOrElse("")),
          _.disabled <-- isEditing.signal.map(!_)
        ).amend(padding := "1rem")

        val facebookUrl = Textfield(
          _.label("Facebook Page Url"),
          _.value(details.facebookUrl.getOrElse("")),
          _.disabled <-- isEditing.signal.map(!_)
        ).amend(padding := "1rem")

        val description = Textarea(
          _.value(details.description.getOrElse("")),
          _.label("Description"),
          _.disabled <-- isEditing.signal.map(!_)
        ).amend(padding := "1rem", display.flex)

        Card(
          cont => {
            society.pictureFileId match {
              case Some(value) =>
                cont
                  .Content
                  .media(FileServiceClient.downloadFileEndpoint.href(value))
              case None => emptyMod
            }
          },
          _.CardHeader(
            _.title(society.details.name),
            _.subtitle(society.details.publicEmailAddress.getOrElse(""))
          ),
          _.Content(
            div(
              cls := "edit-grid",
              publicEmailAddress,
              webPageUrl,
              facebookUrl
            ),
            description
          ),
          _.Content(editButton)
        ).amend(idAttr := ComponentID("mwc-card").toString())

      }
      case None => div("Loading")
    }

  }

  private def setDrawerHeader = contextProvider
    .getContext()
    .map(_.protectedInfo) --> (uiComponents
    .drawer
    .onUserInfoSetHeader(_))

  override def route(): Route[PageT, Unit] = Route(
    encode = page => (),
    decode = _ => RootPage(),
    pattern = root / endOfSegments
  )

}
