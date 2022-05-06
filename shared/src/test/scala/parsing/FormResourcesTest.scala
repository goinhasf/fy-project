package parsing

import org.scalatest.funspec.AnyFunSpec
import io.circe.parser._
import io.circe.syntax._
import dao.forms.FormResourceFieldDescriptor
import io.circe.Json
import io.circe.JsonObject
import dao.forms.SingleFormResourceFieldDescriptor
import dao.forms.LoopFormResourceFieldDescriptor
import dao.events.EventWizardQuestion
import dao.events.EventWizardQuestionState
import dao.events.QuestionChoice

class FormResourcesTest extends AnyFunSpec {

  import TestUtils._

  it("should correctly parse all cases of FormResourceFieldDescriptor") {
    val simpleSingleFormResource = parse("""
      {
        "cardinality": "single",
        "field": {
            "date": "date"
        }
      }
    """).map(_.as[FormResourceFieldDescriptor]).fold(fail(_), identity)

    val complexSingleFormResource = parse("""
      {
        "cardinality": "single",
        "field": {
            "userInfo": {
              "firstName": "string"
            }            
        }
      }
    """).map(_.as[FormResourceFieldDescriptor]).fold(fail(_), identity)

    val expectedSingleFormResourceFieldDescriptor =
      SingleFormResourceFieldDescriptor(
        parse("""{"date": "date"}""").getOrElse(fail())
      )

    println(expectedSingleFormResourceFieldDescriptor.descriptorToJsonObject())

    assert(
      simpleSingleFormResource match {
        case Left(value) => fail(value)
        case Right(value) =>
          value == expectedSingleFormResourceFieldDescriptor
      }
    )

    assertResult(
      Seq("date")
    )(expectedSingleFormResourceFieldDescriptor.path)

    val expectedComplexSingleFormResourceFieldDescriptor =
      SingleFormResourceFieldDescriptor(
        parse("""{"userInfo": {"firstName": "string"}}""")
          .getOrElse(fail())
      )

    println(
      expectedComplexSingleFormResourceFieldDescriptor.descriptorToJsonObject()
    )

    assert(
      complexSingleFormResource match {
        case Left(value) => fail(value)
        case Right(value) =>
          value == expectedComplexSingleFormResourceFieldDescriptor
      }
    )

    assertResult(
      Seq("userInfo", "firstName")
    )(expectedComplexSingleFormResourceFieldDescriptor.path)

    val loopFormResource = parse("""
      {
        "cardinality": "loop",
        "field": {
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
        }
    }
    """).map(_.as[FormResourceFieldDescriptor]).fold(fail(_), identity)

    val expectedLoopFormResourceDescriptor = LoopFormResourceFieldDescriptor(
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

    assertResult(
      List(
        List("items", "date"),
        List("items", "description"),
        List("items", "cost")
      )
    )(
      expectedLoopFormResourceDescriptor.paths
    )

    println("Items")
    println(
      expectedLoopFormResourceDescriptor.descriptorToJsonObject(
        Map(
          Seq("date")        -> "someDate".asJson,
          Seq("description") -> "someDesc".asJson,
          Seq("cost")        -> 12.asJson
        )
      )
    )

    assert(
      loopFormResource match {
        case Left(value) => fail(value)
        case Right(value) =>
          value == expectedLoopFormResourceDescriptor
      }
    )
  }

  it("should correctly convert FormResourceFieldDescriptors to JsonObjects") {
    val expectedSimpleSingleFormResourceFieldDescriptor =
      SingleFormResourceFieldDescriptor(
        parse("""{"date": "date"}""").getOrElse(fail())
      )

    assertResult(JsonObject("date" -> "date".asJson))(
      expectedSimpleSingleFormResourceFieldDescriptor.descriptorToJsonObject()
    )

    val expectedComplexSingleFormResourceFieldDescriptor =
      SingleFormResourceFieldDescriptor(
        parse("""{
          "userInfo": {
                "lastName": "string"
            }
          }""").getOrElse(fail())
      )

    assertResult(
      JsonObject(
        "userInfo" -> JsonObject("lastName" -> "string".asJson).asJson
      )
    )(expectedComplexSingleFormResourceFieldDescriptor.descriptorToJsonObject())
  }

  it("should transform FormResourceFieldDescriptor into JsonObject") {

    val result = parse(formResourceFieldDescriptor) match {
      case Left(value) => fail(value)
      case Right(value) =>
        value.as[Seq[FormResourceFieldDescriptor]] match {
          case Left(value) => fail(value)
          case Right(value) =>
            println(
              value
                .map(_.descriptorToJsonObject)
                .foldRight(JsonObject.empty)(_ deepMerge _)
                .asJson
            )
        }
    }
  }

  it("should parse a form resource question") {
    println(
      decode[EventWizardQuestionState](
        EventWizardQuestionState(
          "",
          "",
          "",
          Some(QuestionChoice("abc", Some(JsonObject("date" -> 1.asJson))))
        ).asJson.noSpaces
      )
    )
  }

  it(
    "should recognize the select values field and turn the input into a select element"
  ) {
    val singleField: SingleFormResourceFieldDescriptor = parse("""
            {
                "cardinality": "single",
                "field": {
                    "society": {
                        "name": "string"
                    }
                },
                "acceptedValues": [
                  "a", 
                  "b"
                ]
            }
        """)
      .fold(fail(_), identity)
      .as[SingleFormResourceFieldDescriptor]
      .fold(fail(_), identity)

    assertResult(Option(Seq("a", "b").asJson)) {
      singleField.acceptedValues
    }
  }

}
