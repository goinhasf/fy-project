package shared.pages
import endpoints4s.algebra.MuxRequest
import io.circe.generic.semiauto._
import shared.pages.content._

sealed trait PageContext extends MuxRequest
object NoContext extends PageContext {
  type Response = EmptyContent
}
final object RootPageContext extends PageContext {
  type Response = RootPageContent
}
final object FormResourcesContext extends PageContext {
  type Response = FormResourcesContent
}
final object UserAccountContext extends PageContext {
  type Response = ProtectedPageContent
}
final object UserRegistrationContext extends PageContext {
  type Response = EmptyContent
}
final object SocietyRegistrationContext extends PageContext {
  type Response = ProtectedPageContent
}
final object SelectSocietyContext extends PageContext {
  type Response = SelectSocietyContent
}
final object EventsContext extends PageContext {
  type Response = EventsContent
}
final object CreateEventsContext extends PageContext {
  type Response = ProtectedPageContent
}
object PageContext {
  implicit val codec = deriveCodec[PageContext]
}
