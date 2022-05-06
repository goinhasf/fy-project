package views.events

import components.card.Card
import dao.events.EventWizardDescriptor
import dao.events.EventWizardState
import components.list.item.MaterialListItem
import components.list.item.builders.SingleLineTextElement
import components.list.item.builders.BeforeListItemIcon
import components.list.item.builders.EmptyAfterListElement
import com.raquo.laminar.api.L._
import components.list.item.builders.ListItemTextElement
import components.list.MaterialList
import components.list.item.builders.AfterListItemIcon
import com.raquo.waypoint.Router
import shared.pages.Page
import views.events.wizard.EventWizardQuestionStateView
import shared.pages.CreateEventPage
import shared.pages.EventWizardStatePage
import services.EventWizardClient

object DraftEventsCard {

  private var numberOfDrafts = 0

  case class Item(
      eventWizardState: EventWizardState
  ) extends MaterialListItem()
      with ListItemTextElement
      with BeforeListItemIcon
      with EmptyAfterListElement
      with AfterListItemIcon {

    def textElement: HtmlElement = div(
      cls := "items-grid",
      h4("Event Draft"),
      span(
        marginTop.auto,
        marginBottom.auto,
        overflow.hidden,
        textOverflow.ellipsis,
        color := "var(--mdc-theme-text-secondary-on-background)",
        s"#${eventWizardState._id}"
      )
    )
    val beforeListItemIcon: Var[String] = Var("mode_edit")
    val afterListItemIcon: Var[String]  = Var("delete")

  }

  def apply(
      drafts: Seq[EventWizardState]
  )(implicit router: Router[Page]): Card = {
    val list = new MaterialList[Item] {

      drafts.foreach(state => addElement(Item(state)))

      override def addElement(e: Item): Unit = {
        super.addElement(
          e.editRoot(
            onClick.mapTo(
              EventWizardStatePage(
                e.eventWizardState._descId,
                e.eventWizardState._id
              )
            ) --> {
              router.pushState _
            }
          ).editRoot(
            _.afterListElement.events(onClick) --> { _ =>
              EventWizardClient
                .deleteEventWizardState(
                  e.eventWizardState._descId,
                  e.eventWizardState._id
                )
                .map {
                  _ match {
                    case Some(value) => {
                      this
                        .listItems
                        .update(_.filter(_.id.toString() != e.id.toString()))
                    }
                    case None =>
                  }
                }
            }
          )
        )
      }
    }

    new Card(
      "Draft Events",
      drafts match {
        case head :: next =>
          "Here you'll see a list of events you're currently drafting"
        case Nil => "No drafts yet"
      },
      drafts match {
        case head :: next => Some(list)
        case Nil          => None
      }
    )
  }
}
