package services.setup

import org.scalatest.funspec.AnyFunSpec
import io.circe.syntax._
import io.circe.parser._
import dao.forms.FormResourceFieldDescriptor
import dao.events.JsonInputQuestionResolver
import org.bson.types.ObjectId
import io.circe.SeqDecoder
class ParsingTest extends AnyFunSpec {
  val firstQuestion = GenericEventWizardQuestions("a", "b", "c").head
  val jsonInputQuestionResolver = JsonInputQuestionResolver(
    new ObjectId().toHexString(),
    "details",
    parse("""
            [
              {
                "cardinality": "single",
                "field": {
                  "event_name": "string"
                }
              },
              {
                "cardinality": "single",
                "field": {
                  "start_date": "string"
                }
              },
              {
                "cardinality": "single",
                "field": {
                  "end_date": "string"
                }
              },
              {
                "cardinality": "single",
                "field": {
                  "start_time": "string"
                }
              },
              {
                "cardinality": "single",
                "field": {
                  "end_time": "string"
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
                  "repeats": "number"
                }
              },
              {
                "cardinality": "single",
                "field": {
                  "repeat_frequency_unit": "string"
                },
                "acceptedValues": [
                  "Weekly",
                  "Monthly",
                  "Yearly"
                ]
              }
            ]
        """)
      .flatMap(_.as[Seq[FormResourceFieldDescriptor]])
      .fold(throw _, identity),
    Some("")
  )

}
