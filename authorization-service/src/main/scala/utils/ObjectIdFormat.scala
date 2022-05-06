package utils

import org.mongodb.scala.bson.ObjectId
import play.api.libs.json._
import scala.util.Try

object ObjectIdFormatJsonMacro extends Format[ObjectId] {

  def writes(objectId: ObjectId): JsValue = JsString(objectId.toString)
  def reads(json: JsValue): JsResult[ObjectId] = json match {
    case JsString(x) => {
      val maybeOID: Try[ObjectId] = Try{new ObjectId(x)}
      if(maybeOID.isSuccess) JsSuccess(maybeOID.get) else {
        JsError("Expected ObjectId as JsString")
      }
    }
    case _ => JsError("Expected ObjectId as JsString")
  }
}