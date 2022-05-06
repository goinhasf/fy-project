package services

import shared.endpoints.events.wizard.SocietyEventsEndpoints
import endpoints4s.xhr
import services.util.DefaultCsrfTokenConsumer
import dao.events.Reviewed

object SocietyEventsClient
    extends ClientService
    with XHRObservableEndpoints
    with SocietyEventsEndpoints
    with xhr.JsonEntitiesFromCodecs
    with DefaultCsrfTokenConsumer {
  def reviewEvent(id: String, status: Reviewed) =
    super.reviewEvent(id, status, getCsrfToken().get)
}
