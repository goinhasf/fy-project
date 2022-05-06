package services.endpoints

import play.api.routing.SimpleRouter
import play.api.routing.Router
import com.google.inject._

@Singleton
case class ApiEndpoints @Inject() (
    authService: LoginEndpointService,
    adminPagesEndpoints: AdminPagesEndpoints,
    formResourceApi: FormResourceEndpointService,
    appContext: PageContextEndpointsService,
    pageEndpoints: PagesEndpointService,
    societyService: SocietiesService,
    eventWizardService: EventWizardEndpoints,
    societyEventsEndpoints: SocietyEventsEndpointRoutes,
    formSubmissionEndpoints: FormSubmissionEndpoints
) extends SimpleRouter {
  def routes: Router.Routes = authService
    .routes
    .orElse(formResourceApi.routes)
    .orElse(appContext.routes)
    .orElse(pageEndpoints.routes)
    .orElse(societyService.routes)
    .orElse(eventWizardService.routes)
    .orElse(societyEventsEndpoints.routes)
    .orElse(adminPagesEndpoints.routes)
    .orElse(formSubmissionEndpoints.routes)
}
