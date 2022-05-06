package services.contexts

import com.google.inject._
import data.repositories.FormResourceRepository
import shared.pages.content.FormResourcesContent
import scala.concurrent.Future
import shared.pages.content.ProtectedPageContent
import endpoints4s.play.server.PlayComponents

@Singleton()
class FormResourcesContextService @Inject() (
    formResourcesRepository: FormResourceRepository,
    pc: PlayComponents
) extends PageContextService[FormResourcesContent] {
  def getContent(implicit
      protectedContent: ProtectedPageContent
  ): Future[FormResourcesContent] = formResourcesRepository
    .findAllResources(protectedContent.userInfo.id)
    .map(resources =>
      FormResourcesContent(resources, protectedContent)
    )(pc.executionContext)

}
