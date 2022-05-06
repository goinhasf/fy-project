package shared.endpoints

import endpoints4s.algebra

trait MultipartFormEndpoints extends algebra.Endpoints {

  type File
  type FormData

  protected def multipartFormRequestEntity: RequestEntity[FormData]
  protected def fileResponseEntity: ResponseEntity[File]
}
