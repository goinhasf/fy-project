package services

import shared.endpoints.FormResourceEndpoints
import endpoints4s.xhr
import endpoints4s.algebra
import org.scalajs.dom._
import scalajs.js
import scala.io.Source
import endpoints4s.Tupler
import scala.scalajs.js._
import components.form.{Form => FormImpl}
import util.DefaultCsrfTokenConsumer
import dao.forms.FormResource
import com.raquo.airstream.core.EventStream
import scala.scalajs.js.typedarray.ArrayBuffer
import endpoints4s.Invalid
import dao.forms.FormResourceDetails
import io.circe.JsonObject
import io.circe.syntax._
import components.form.JsonFormPart

object FormResourceService
    extends ClientService
    with FormResourceEndpoints
    with XHRObservableEndpoints
    with MultipartFormEntities
    with xhr.JsonEntitiesFromCodecs
    with DefaultCsrfTokenConsumer {

  def insertResource(
      form: FormData
  ): EventStream[Either[ClientErrors, FormResource]] = super
    .insertResource((form, getCsrfToken().get))

  def updateFormResource(
      resourceId: String,
      resource: FormResourceDetails
  ): EventStream[Option[FormResource]] =
    super.updateFormResource((resourceId, resource, getCsrfToken().get))

  def updateFormAnswers(
      resourceId: String,
      answers: JsonObject
  ) = super.updateDefaultFieldValues(
    resourceId,
    answers,
    getCsrfToken().get
  )

  def getFormResourceAsPdf(resourceId: String, answers: Option[JsonObject]) =
    super
      .formResourceAsPdf
      .href(resourceId, answers.map(_.asJson))

}
