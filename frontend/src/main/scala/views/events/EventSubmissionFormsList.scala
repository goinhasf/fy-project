package views.events

import com.raquo.laminar.api.L._
import shared.endpoints.GetFormSubmission
import components.list.item.MaterialListItem
import components.list.item.builders.ListItemTextElement
import components.list.item.builders.EmptyBeforeListElement
import components.list.item.builders.AfterListItemIcon
import dao.forms.Ready
import dao.forms.Submitted
import dao.forms.InReview
import dao.forms.Reviewed
import components.list.MaterialList
import shared.pages.Page
import shared.pages.FormSubmissionDetailsPage

case class FormSubmissionItem(sub: GetFormSubmission)
    extends MaterialListItem
    with ListItemTextElement
    with EmptyBeforeListElement
    with AfterListItemIcon {

  val afterListItemIcon: Var[String] = Var(determineIconType)
  def textElement: HtmlElement = div(
    display.inlineFlex,
    span(sub.formResource.details.name, margin := "auto 0.25rem auto 0"),
    span(margin := "auto 0 auto 0", s"#${sub.formResource._id}", opacity(0.6))
  )

  private def determineIconType = {

    sub.submission.formSubmissionStatus match {
      case Ready()                                   => "pending"
      case Submitted(_submittedById, submissionDate) => "outbox"
      case InReview(
            _submittedById,
            _reviewerId,
            submissionDate,
            reviewStartDate
          ) =>
        "group"
      case Reviewed(
            _submittedById,
            _reviewerId,
            submissionDate,
            reviewStartDate,
            reviewEndDate,
            changesRequested,
            notes
          ) =>
        "done"
    }
  }

}

case class EventSubmissionFormsList(state: Seq[GetFormSubmission])
    extends MaterialList[FormSubmissionItem]() {

  val elementClickBus = new EventBus[Page]

  state.map(FormSubmissionItem.apply).foreach(addElement)

  override def addElement(e: FormSubmissionItem): Unit = {
    super.addElement(
      e.editRoot(
        onClick
          .mapTo(e.sub)
          .map(sub =>
            FormSubmissionDetailsPage(
              sub.submission._id
            )
          ) --> elementClickBus
      )
    )
  }
}
