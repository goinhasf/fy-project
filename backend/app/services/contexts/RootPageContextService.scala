package services.contexts

import shared.pages.content.RootPageContent
import com.google.inject._
import scala.concurrent.Future
import shared.pages.content.ProtectedPageContent
import endpoints4s.play.server.PlayComponents

class RootPageContextService @Inject() (
    pc: PlayComponents
) extends PageContextService[RootPageContent] {
  def getContent(implicit
      protectedContent: ProtectedPageContent
  ): Future[RootPageContent] = Future.successful(
    RootPageContent(
      "hello there",
      protectedContent
    )
  )
}
