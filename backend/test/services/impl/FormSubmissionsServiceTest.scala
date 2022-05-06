package services.impl

import org.scalatest.funspec.AnyFunSpec
import org.mongodb.scala._
import dao.forms.Submitted
import dao.forms.Ready
import io.circe.parser._
import io.circe.syntax._
import dao.forms.FormSubmissionStatus
import dao.forms.InReviewType
import dao.forms.SubmittedType
import dao.forms.StatusType
import dao.forms.ReadyType

class FormSubmissionsServiceTest extends AnyFunSpec {

  val statuses: Seq[StatusType] =
    Seq(SubmittedType(), ReadyType(), InReviewType())

  val filter = Document(
    "$and" -> statuses.map(status =>
      Document(
        "$or" -> Document(
          "formSubmissionStatus.statusType" -> Document(
            status.asJson.noSpaces
          )
        )
      )
    )
  )

  println(filter.toJson())
}
