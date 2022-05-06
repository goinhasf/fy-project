package views.events.wizard

import shared.endpoints.events.wizard.ops.GetEventWizardState
import components.list.MaterialList
import com.raquo.laminar.api.L._
import shared.endpoints.events.wizard.ops.GetEventWizardQuestionState
import components.list.item.MaterialListItem
import components.list.item.builders.EmptyBeforeListElement
import components.list.item.builders.ListItemTextElement
import components.list.item.builders.AfterListItemIcon
import shared.pages.EventWizardQuestionStatePage
import shared.pages.Page

case class QuestionItem(index: Int, question: GetEventWizardQuestionState)
    extends MaterialListItem
    with ListItemTextElement
    with EmptyBeforeListElement
    with AfterListItemIcon {

  val afterListItemIcon: Var[String] = Var(determineIconType)
  def textElement: HtmlElement = div(
    display.inlineFlex,
    h2(s"$index.", marginRight := "0.75rem"),
    span(margin := "auto 0 auto 0", question.question.title)
  )

  private def determineIconType = {
    if (question.state.isDefined) {
      "done"
    } else {
      "pending"
    }
  }

}

case class WizardQuestionsList(state: GetEventWizardState)
    extends MaterialList[QuestionItem]() {

  val elementClickBus = new EventBus[Page]

  var index = state.questions.size
  state.questions.reverse.foreach { q =>
    addElement(QuestionItem(index, q))
    index -= 1
  }

  override def addElement(e: QuestionItem): Unit = {
    super.addElement(
      e.editRoot(
        onClick
          .mapTo(e.question)
          .map(question =>
            EventWizardQuestionStatePage(
              state.state._descId,
              state.state._id,
              question.question._id
            )
          ) --> elementClickBus
      )
    )
  }
}
