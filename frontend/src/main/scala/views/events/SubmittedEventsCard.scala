package views.events

import components.card.Card
import com.raquo.laminar.api.L._
import dao.events.SocietyEvent
import com.github.uosis.laminar.webcomponents.material
import shared.pages.EventSubmissionDetailsPage
import com.raquo.waypoint.Router
import shared.pages.Page
import shared.endpoints.events.wizard.GetSocietyEvent

object SubmittedEventsCard {

  def itemFactory(
      societyEvent: GetSocietyEvent
  )(implicit router: Router[Page]): material.List.ListItem.El =
    material
      .List
      .ListItem(
        _.value(societyEvent.event._id),
        _.twoline(true),
        _.slots.default(span(societyEvent.event.details.name)),
        _.slots.secondary(span(societyEvent.event.details.eventType.name)),
        _.slots.graphic(i("event"))
      )
      .amend(
        onClick.mapTo(EventSubmissionDetailsPage(societyEvent.event._id)) --> {
          router.pushState(_)
        }
      )

  def apply(title: String, subtitle: String, seq: Seq[GetSocietyEvent])(implicit
      router: Router[Page]
  ) = {

    val listItems = seq.map(itemFactory).reverse

    val cardContent: HtmlElement =
      listItems.foldRight(material.List())((item, list) =>
        list.amend(material.List.slots.default(item))
      )
    new Card(
      title,
      subtitle,
      if (seq.isEmpty) {
        Some(span(opacity(0.6), "None yet"))
      } else {
        Some(cardContent)
      }
    )
  }

}
