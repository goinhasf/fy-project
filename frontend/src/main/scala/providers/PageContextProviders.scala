package providers

import shared.pages.RootPageContext
import scala.concurrent.Future
import shared.pages.content.EmptyContent
import shared.pages.content.RootPageContent
import com.raquo.airstream.core.EventStream
import shared.pages.content.FormResourcesContent
import shared.pages.FormResourcesContext
import shared.pages.content.ProtectedPageContent
import shared.pages.SocietyRegistrationContext
import shared.pages.content.SelectSocietyContent
import shared.pages.SelectSocietyContext
import shared.pages.content.EventsContent
import shared.pages.EventsContext

object PageContextProviders {

  implicit val noContextProvider = new ContextProvider[EmptyContent] {
    def getContext(): EventStream[EmptyContent] = EventStream
      .fromValue(EmptyContent())
  }
  implicit val protectedContextProvider =
    new ContextProvider[ProtectedPageContent] {
      def getContext(): EventStream[ProtectedPageContent] =
        getPageContext((SocietyRegistrationContext, getCsrfToken().get))
          .collect({ case content: ProtectedPageContent =>
            content
          })
    }

  implicit val rootContextProvider = new ContextProvider[RootPageContent] {
    def getContext() =
      getPageContext((RootPageContext, getCsrfToken().get)).collect({
        case content: RootPageContent => content
      })
  }
  implicit val formResourcesProvider =
    new ContextProvider[FormResourcesContent] {
      def getContext() =
        getPageContext((FormResourcesContext, getCsrfToken().get)).collect({
          case content: FormResourcesContent => content
        })
    }
  implicit val selectSocietyProvider =
    new ContextProvider[SelectSocietyContent] {
      def getContext() =
        getPageContext((SelectSocietyContext, getCsrfToken().get)).collect({
          case content: SelectSocietyContent => content
        })
    }
  implicit val eventsContextProvider =
    new ContextProvider[EventsContent] {
      def getContext() =
        getPageContext((EventsContext, getCsrfToken().get)).collect({
          case content: EventsContent => content
        })
    }
}
