package services

import scala.scalajs.js._

import components.form.Form
import endpoints4s.xhr
import org.scalajs.dom.raw.XMLHttpRequest
import shared.endpoints.MultipartFormEndpoints
import org.scalajs.dom.raw.Blob
import org.scalajs.dom.raw.FileReader
import org.scalajs.dom.window
import org.scalajs.dom.raw.BlobPropertyBag

trait MultipartFormEntities extends xhr.Endpoints with MultipartFormEndpoints {

  type File     = String
  type FormData = Form

  override def multipartFormRequestEntity
      : Function2[FormData, XMLHttpRequest, Any] =
    (form, request) => form.formData

  protected def fileResponseEntity: ResponseEntity[File] = req => {
    Either.cond(
      req.responseText != null,
      req.responseText,
      new Throwable("No file response received")
    )
  }

}
