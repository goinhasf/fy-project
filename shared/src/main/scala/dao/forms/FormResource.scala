package dao.forms
import java.util.Date
import dao.Ownership
import java.io.File
import io.circe.generic.semiauto._
import io.circe.JsonObject
import io.circe.Json
import io.circe.generic.JsonCodec

@JsonCodec
case class FormResourceDetails(
    name: String,
    notes: String,
    categories: List[FormCategory],
    isPublic: Boolean,
    fileId: Option[String] = None
)

@JsonCodec
case class FormResource(
    _id: String,
    details: FormResourceDetails,
    resourceOwnership: Ownership,
    fields: Seq[FormResourceFieldDescriptor],
    defaultFieldValues: Option[JsonObject] = None
)
