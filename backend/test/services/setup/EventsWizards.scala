package services.setup

import dao.events._
import org.bson.types.ObjectId
import io.circe.parser._
import io.circe.syntax._
import dao.forms.FormResourceFieldDescriptor
import io.circe.Json

object GenericEventWizard {

  def apply(
      questions: Seq[EventWizardQuestion]
  ) = EventWizardDescriptor(
    "606acdf35cc6ad26dfa6864c",
    SocietyEventType("Social"),
    questions.map(_._id)
  )
}

object GenericEventWizardQuestions {

  import FormResourceObjects._

  def apply(
      ticketsFormResourceId: String,
      externalPaymentsFormId: String,
      riskAssessmentFormId: String
  ): Seq[EventWizardQuestion] = {

    lazy val firstQuestion = EventWizardQuestion(
      new ObjectId().toHexString(),
      "Select type of event",
      Map(
        "Social" -> JsonInputQuestionResolver(
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
                  "start_date": "date"
                }
              },
              {
                "cardinality": "single",
                "field": {
                  "end_date": "date"
                }
              },
              {
                "cardinality": "single",
                "field": {
                  "start_time": "time"
                }
              },
              {
                "cardinality": "single",
                "field": {
                  "end_time": "time"
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
                  "None",
                  "Weekly",
                  "Monthly",
                  "Yearly"
                ]
              }
            ]
        """)
            .flatMap(_.as[Seq[FormResourceFieldDescriptor]])
            .fold(throw _, identity),
          Some(numberOfPeopleQuestion._id)
        )
        // "External Speakers/Instructors/Films/Debates" -> null,
        // "Trips"                                       -> null
      )
    )

    lazy val numberOfPeopleQuestion = EventWizardQuestion(
      new ObjectId().toHexString(),
      "Expected Number of people",
      Map(
        "Less than 20" -> NextQuestionResolver(
          new ObjectId().toHexString(),
          Some(hostingItAtUobQuestion._id),
          Map(
            "Note" ->
              """
            You can use http://app.matrixbooking.com/ to book a room using your group's email account. 
            If your group hasn't got an account yet, please contact us and we'll get you set up.
            """
          )
        ),
        "Between 20 and 50" -> NextQuestionResolver(
          new ObjectId().toHexString(),
          Some(hostingItAtUobQuestion._id),
          Map(
            "Note" ->
              """
            You can use http://app.matrixbooking.com/ to book a room using your group's email account. 
            If your group hasn't got an account yet, please contact us and we'll get you set up.
            """
          )
        ),
        "Between 50 and 100" -> NextQuestionResolver(
          new ObjectId().toHexString(),
          Some(hostingItAtUobQuestion._id),
          Map(
            "Note" ->
              """
            You can use http://app.matrixbooking.com/ to book a room using your group's email account. 
            If your group hasn't got an account yet, please contact us and we'll get you set up.
            """
          )
        ),
        "Greater than 100" -> NextQuestionResolver(
          new ObjectId().toHexString(),
          Some(hostingItAtUobQuestion._id),
          Map(
            "Important" ->
              """
              You may need to contact student groups to arrange such venue. This may also cost you some money 
              unlike other smaller venues at UoB.
              """
          )
        )
      )
    )

    lazy val hostingItAtUobQuestion = EventWizardQuestion(
      new ObjectId().toHexString(),
      "Are you thinking of hosting it at UoB?",
      Map(
        "Yes" -> NextQuestionResolver(
          new ObjectId().toHexString(),
          Some(indoorOrOutdoorsQuestion._id),
          Map(
            "Note" ->
              """
            You can use http://app.matrixbooking.com/ to book a room using your group's email account. 
            If your group hasn't got an account yet, please contact us and we'll get you set up.
            """
          )
        ),
        "No" -> new NextQuestionResolver(
          new ObjectId().toHexString(),
          Some(indoorOrOutdoorsQuestion._id)
        )
      )
    )

    lazy val indoorOrOutdoorsQuestion = EventWizardQuestion(
      new ObjectId().toHexString(),
      "Indoors or Outdoors?",
      Map(
        "Indoors" -> new NextQuestionResolver(
          new ObjectId().toHexString(),
          Some(ticketsQuestion._id)
        ),
        "Outdoors" -> new FormResourcesQuestionResolver(
          new ObjectId().toHexString(),
          Map(
            riskAssessmentFormId -> true
          ),
          Some(ticketsQuestion._id)
        )
      )
    )

    lazy val ticketsQuestion = EventWizardQuestion(
      new ObjectId().toHexString(),
      "Will you require the Guild to issue tickets?",
      Map(
        "Yes" -> new FormResourcesQuestionResolver(
          new ObjectId().toHexString(),
          Map(ticketsFormResourceId -> true),
          Some(requireAnyExternalPurchases._id)
        ),
        "No" -> new NextQuestionResolver(
          new ObjectId().toHexString(),
          Some(requireAnyExternalPurchases._id)
        )
      )
    )

    lazy val requireAnyExternalPurchases = EventWizardQuestion(
      new ObjectId().toHexString(),
      "Will you to pay any external party for goods and services?",
      Map(
        "Yes" -> new FormResourcesQuestionResolver(
          new ObjectId().toHexString(),
          Map(externalPaymentsFormId -> true)
        ),
        "No" -> new NextQuestionResolver(new ObjectId().toHexString())
      )
    )

    Seq(
      firstQuestion,
      numberOfPeopleQuestion,
      hostingItAtUobQuestion,
      indoorOrOutdoorsQuestion,
      ticketsQuestion,
      requireAnyExternalPurchases
    )
  }
}
