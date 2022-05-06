package json

import org.scalatest.funspec.AnyFunSpec
import views.formResourceDetails.FormResourceFieldComponent
import dao.forms.FormResourceFieldDescriptor
import io.circe._
import io.circe.syntax._

class CursorTraverseTest extends AnyFunSpec {
  it("should traverse the object and add all the keys") {
    val json =
      """
        {
            "society": {
                "name": {
                    "test": "string"
                },
                "fold": {
                  "house": "string"
                }                
            },
            "member": {
              "join": {
                "party": {
                  "house": "string"
                }
              }
            }
        }
        """

    val parsed = parser.parse(json).getOrElse(fail())

    def qualifiedNamesRec(cursor: ACursor, acc: Seq[String]): Seq[String] = {
      val keys = cursor.keys.toSeq.flatten
      if (keys.length == 0) {
        acc
      } else {
        keys.flatMap(key =>
          qualifiedNamesRec(cursor.downField(key), acc :+ key)
        )
      }
    }

    def qualifiedNames(json: Json) = {
      def qualifiedNamesRec(cursor: ACursor, acc: Seq[String]): Seq[String] = {
        val keys = cursor.keys.toSeq.flatten
        if (keys.length == 0) {
          acc
        } else {
          keys.flatMap(key =>
            qualifiedNamesRec(cursor.downField(key), acc :+ key)
          )
        }
      }
      qualifiedNamesRec(json.hcursor, Seq())
    }

    println(
      qualifiedNames(parsed)
    )

    def adjustCursor(path: Seq[String], state: JsonObject): JsonObject =
      path
        .foldLeft(Option(state.asJson))((s, path) =>
          s.flatMap(_.asJson.hcursor.downField(path).as[Json].toOption)
        )
        .flatMap(_.asObject)
        .getOrElse(JsonObject.empty)

    val target = parser
      .parse("""
      {
        "forms": {
          "abc": { "name": "nature"}
        }
      }
    """)
      .toOption
      .flatMap(_.asObject)

    target match {
      case Some(value) =>
        println(adjustCursor(Seq("forms", "abc"), value))
      case None => fail("Could not convert target to json object")
    }

  }
}
