package services.contexts

import shared.pages.content.ContextContent
import scala.concurrent.Future
import services.endpoints.handlers.SessionHandler
import shared.pages.content.ProtectedPageContent

trait PageContextService[R <: ContextContent] {
  def getContent(implicit protectedContext: ProtectedPageContent): Future[R]
}
