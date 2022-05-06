package services

import services.util.DefaultCsrfTokenConsumer
import shared.endpoints.FormSubmissionsEndpoints
import endpoints4s.xhr
import shared.endpoints.ChangeSubmissionStatus

object FormSubmissionsClient
    extends XHRObservableEndpoints
    with ClientService
    with DefaultCsrfTokenConsumer
    with FormSubmissionsEndpoints
    with xhr.JsonEntitiesFromCodecs {

  def changeFormSubmissionStatus(
      id: String,
      changeSubStatus: ChangeSubmissionStatus
  ) =
    super.changeFormSubmissionStatus(id, changeSubStatus, getCsrfToken().get)
}
