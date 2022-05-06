package shared.pages.content
import io.circe.generic.semiauto._
import dao.users.UserInfo
import dao.forms.FormResource
import dao.societies.Society
import dao.events.SocietyEvent

sealed trait ContextContent
final case class EmptyContent() extends ContextContent
final case class ProtectedPageContent(
    userInfo: UserInfo,
    society: Option[Society] = None
) extends ContextContent
object ProtectedPageContent {
  implicit val codec = deriveCodec[ProtectedPageContent]
}
final case class RootPageContent(
    message: String,
    protectedInfo: ProtectedPageContent
) extends ContextContent


final case class FormResourcesContent(
  formResources: Seq[FormResource],
  protectedInfo: ProtectedPageContent
) extends ContextContent

final case class SelectSocietyContent(
  userSocieties: Set[Society],
  protectedInfo: ProtectedPageContent
) extends ContextContent

final case class EventsContent(
  userSocieties: Set[SocietyEvent],
  protectedInfo: ProtectedPageContent
) extends ContextContent

object ContextContent {
  implicit val codec = deriveCodec[ContextContent]
}
