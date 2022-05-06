package components

import org.scalatest.funspec.AnyFunSpec
import dao.forms.FormResourceFields
import dao.forms.FormResourceFieldDescriptor
import io.circe.parser._
import io.circe.syntax._

import views.formResourceDetails.SingleFormResourceFieldComponent
import com.raquo.laminar.api.L._
import org.scalajs.dom
import io.circe.JsonObject
import dao.forms.SingleFormResourceFieldDescriptor
import views.formResourceDetails.LoopFormResourceFieldComponent
import dao.forms.LoopFormResourceFieldDescriptor
import io.circe.Json
import io.circe.ACursor
class FormResourceFieldComponentTest extends AnyFunSpec {

  it("should retrieve a json object representing the field") {

    val singleField: SingleFormResourceFieldDescriptor = parse("""
            {
                "cardinality": "single",
                "field": {
                    "society": {
                        "name": "string"
                    }
                }
            }
        """)
      .fold(fail(_), identity)
      .as[SingleFormResourceFieldDescriptor]
      .fold(fail(_), identity)

    val component = new SingleFormResourceFieldComponent(
      singleField,
      Some(
        parse("""
            {
                "society": {
                    "name": "Morning"
                }
            }
        """)
          .fold(fail("Cannot parse json", _), id => id.asObject)
          .getOrElse(fail("Cannot convert to JsonObject"))
      )
    )

    component.input.getValue().set("Hello")

    assertResult(
      JsonObject("society" -> JsonObject("name" -> "Hello".asJson).asJson)
    )(
      component.toJsonObject
    )

    component
      .getValueFromAnswer
      .fold(fail("value was not set"))(assertResult(_)("Morning"))

  }

  it(
    "should get the value from an answer target for LoopFormResourceFieldComponent"
  ) {
    val descriptor = LoopFormResourceFieldDescriptor(
      parse("""
            { 
                "items": [
                  {
                      "cardinality": "single",
                      "field": {
                          "date": "date"
                      }
                  },
                  {
                      "cardinality": "single",
                      "field": {
                          "description": "string"
                      }
                  },
                  {
                      "cardinality": "single",
                      "field": {
                          "cost": "string"
                      }
                  }
              ]
            }""").getOrElse(fail())
    )

    val answer = parse("""  
      { 
          "items": [
            {
              "date": "12/12/12",
              "description": "some desc",
              "cost": 12
            },
            {
              "date": "12/12/12",
              "description": "some desc",
              "cost": 12
            }
        ]
      }""")
      .getOrElse(fail())
      .asObject
      .getOrElse(fail("Cannot Convert to object"))

    def getValuesFromAnswers = for {
      arr <- answer.asJson.hcursor.downField("items").as[Array[Json]].toOption
    } yield makeMapFromArr(arr)

    def makeMapFromArr(arr: Array[Json]) = {
      def tailRecHelper(
          cursor: ACursor,
          acc: Seq[String]
      ): (Seq[String], String) = {
        val cursorKeys = cursor.keys.toSeq.flatten
        if (cursorKeys.length == 0) {
          (acc, cursor.as[Json].map(_.toString()).getOrElse(""))
        } else {
          tailRecHelper(
            cursor.downField(cursorKeys.head),
            acc :+ cursorKeys.head
          )
        }
      }
      def buildEntry(json: Json) = tailRecHelper(json.hcursor, Seq())
      def allKeysAsJson(json: Json) = json
        .hcursor
        .keys
        .toSeq
        .flatten
        .map { key =>
          JsonObject(
            key -> json
              .hcursor
              .downField(key)
              .as[Json]
              .getOrElse(JsonObject.empty.asJson)
          ).asJson
        }

      arr
        .map(allKeysAsJson)
        .map(jsons => { println(jsons); jsons.map(buildEntry) })
        .map(_.toMap)
        .toSeq

    }

    // val component = LoopFormResourceFieldComponent(
    //   descriptor,
    //   Some(FormResourceAnswer("", "", "", "", answer))
    // )
    println(getValuesFromAnswers.get)
  }

}
