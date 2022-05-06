package services
import endpoints4s.xhr
import shared.endpoints.FileUploadEndpoints
object FileServiceClient
    extends XHRObservableEndpoints
    with MultipartFormEntities
    with FileUploadEndpoints
    with ClientAuthentication
    with xhr.JsonEntitiesFromCodecs 
