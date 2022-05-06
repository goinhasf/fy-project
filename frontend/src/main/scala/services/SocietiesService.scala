package services

import shared.endpoints.SocietiesEndpoints
import endpoints4s.xhr
import dao.societies.SocietyDetails
import services.util.DefaultCsrfTokenConsumer

object SocietiesService
    extends ClientService
    with XHRObservableEndpoints
    with SocietiesEndpoints
    with MultipartFormEntities
    with xhr.JsonEntitiesFromCodecs
    with DefaultCsrfTokenConsumer {

  def createSociety(societyDetailsForm: FormData) = super.createSociety(
    (societyDetailsForm, getCsrfToken().get)
  )
}
