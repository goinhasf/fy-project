package services.contexts

import com.google.inject._

@Singleton()
case class AllContextServices @Inject() (
    formResourcesContextService: FormResourcesContextService,
    rootPageContextService: RootPageContextService
)
