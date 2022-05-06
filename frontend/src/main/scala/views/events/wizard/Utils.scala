package views.events.wizard

import io.circe.JsonObject
import io.circe.syntax._
import io.circe.Json
import io.circe.ACursor

object Utils {
  def adjustCursor(path: Seq[String], state: JsonObject): JsonObject =
    path
      .foldLeft(Option(state.asJson))((s, path) =>
        s.flatMap(_.asJson.hcursor.downField(path).as[Json].toOption)
      )
      .flatMap(_.asObject)
      .getOrElse(JsonObject.empty)

  def flatten(cursor: ACursor): Json = {
    val keys = cursor.keys.toSeq.flatten

    if (keys.length == 0) { JsonObject.empty.asJson }
    else if (keys.length == 1) {
      cursor.get[Json](keys.head).getOrElse(JsonObject.empty.asJson)
    } else {
      keys
        .map(key => flatten(cursor.downField(key)))
        .foldRight(JsonObject.empty.asJson)(_ deepMerge _)
    }
  }

  def addPath(path: Seq[String], json: JsonObject): Json = {
    def helper(
        path: Seq[String],
        json: JsonObject,
        acc: JsonObject
    ): JsonObject = {
      if (path.length == 1) {
        acc.add(path.head, flatten(json.asJson.hcursor))
      } else {
        acc.add(path.head, helper(path.tail, json, JsonObject.empty).asJson)
      }
    }
    helper(path, json, JsonObject.empty).asJson
  }

}
