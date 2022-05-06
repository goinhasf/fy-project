package views.events.wizard

import components.BaseUIComponents
import providers.ContextProvider
import views.ViewImplWithNav
import shared.pages.EventWizardStatePage
import shared.pages.content.ProtectedPageContent
import scala.reflect.ClassTag
import com.raquo.laminar.api.L._
import com.raquo.waypoint.Route
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.raquo.waypoint.Router
import org.scalajs.dom.raw.HTMLElement
import shared.pages.Page
import urldsl.language.dummyErrorImpl._
import shared.endpoints.events.wizard.ops.GetEventWizardQuestionState
import shared.pages.EventWizardSubmitPage
import shared.endpoints.events.wizard.ops.GetEventWizardState
import io.circe.JsonObject
import io.circe.syntax._
import services.EventWizardClient
import components.card.ActionableCard
import components.card.CardActionButton
import components.card.Card
import components.button.MaterialButton
import components.button.label.IconButtonLabel
import components.button.style.ButtonStyles
import shared.pages.EventWizardQuestionStatePage
import components.navigation.appbar.TopAppBar
import components.DisplayFabEvent
import shared.pages.EventsPage
import endpoints4s.Invalid

class EventWizardSubmissionView(
    implicit val uiComponents: BaseUIComponents,
    override implicit val contextProvider: ContextProvider[ProtectedPageContent]
) extends ViewImplWithNav[(String, String), ProtectedPageContent] {

  type PageT = EventWizardSubmitPage
  implicit val tag: ClassTag[PageT] = ClassTag(classOf[EventWizardSubmitPage])
  def route(): Route[EventWizardSubmitPage, (String, String)] = Route(
    p => p.args,
    EventWizardSubmitPage.tupled(_),
    pattern = root / "event-wizard" / segment[String] / "state" / segment[
      String
    ] / "submit" / endOfSegments
  )

  def renderContent(pageT: Signal[EventWizardSubmitPage])(implicit
      router: Router[Page]
  ): HtmlElement = {

    uiComponents.topAppBar.title.set("Submit for approval")
    uiComponents.eventBus.emit(DisplayFabEvent(false))
    uiComponents.topAppBar.primaryButton.set(TopAppBar.BackButton)

    div(
      cls := "content",
      child <-- pageT
        .map(_.args)
        .flatMap(EventWizardClient.getEventWizardState)
        .collect({ case Some(v) => v })
        .map(content),
      setDrawerHeader
    )
  }

  def content(
      state: GetEventWizardState
  )(implicit router: Router[Page]): HtmlElement = {

    val errorMessageBus = new EventBus[String]

    div(
      h1("Check your answers"),
      createQuestionResponseItems(state),
      div(padding := "1rem", color := "var(--mdc-theme-error)", child.text <-- errorMessageBus),
      MaterialButton(
        IconButtonLabel("Submit", "send"),
        ButtonStyles.raisedButtonStyle
      ).amend(
        onMountBind { ctx =>
          onClick.mapTo(()) --> { _ =>
            EventWizardClient
              .submitEventWizard(state.state._descId, state.state._id)
              .recover({ case t: Throwable =>
                Option(Left(Invalid(t.getMessage())))
              })
              .foreach { id =>
                id match {
                  case Left(value) =>
                    if (value.errors.reduceRight(_ + _).contains("basicInfo")) {
                      errorMessageBus.emit(
                        "Please go back to the first question and make sure everything has been filled in"
                      )
                    } else {
                      errorMessageBus.emit(
                        "An unknown error occurred. Try refreshing the page."
                      )
                    }
                  case Right(value) => {
                    errorMessageBus.emit("")
                    router.pushState(EventsPage())
                  }

                }
              }(ctx.owner)
          }
        }
      ),
      uiComponents
        .topAppBar
        .onBackButtonClicked
        .mapTo(
          EventWizardStatePage(
            state.state._descId,
            state.state._id
          )
        ) --> { router.pushState(_) }
    )
  }

  private def createQuestionResponseItems(
      state: GetEventWizardState
  )(implicit router: Router[Page]): HtmlElement = {

    var questionNumber = state.questions.size + 1
    state.questions.reverse.foldRight(div()) { (question, el) =>
      questionNumber -= 1
      val questionTitle = question.question.title
      val questionState =
        question.state.map(_.choice).getOrElse("Not yet answered")

      val information =
        div(
          display.inlineFlex,
          p("Choice:", marginRight := "0.25rem"),
          b(margin.auto, questionState)
        )

      val card = div(
        cls := Card.outlined,
        marginBottom := "0.5rem",
        div(
          cls := "edit-grid",
          div(
            cls := "mdc-card-wrapper__text-section",
            div(cls := "mdc-card__title", questionTitle),
            div(cls := "mdc-card__subtitle", b(margin.auto, questionState))
          ),
          MaterialButton(
            IconButtonLabel("Change", "edit"),
            ButtonStyles.outlinedButtonStyle
          ).editRoot(margin := "auto 1rem 0.5rem auto")
            .editRoot(
              onClick
                .mapTo(
                  EventWizardQuestionStatePage(
                    state.state._descId,
                    state.state._id,
                    question.question._id
                  )
                ) --> { router.pushState(_) }
            )
        )
      )
      el.amend(card)
    }
  }

  private def setDrawerHeader = contextProvider
    .getContext() --> (uiComponents
    .drawer
    .onUserInfoSetHeader(_))

}
