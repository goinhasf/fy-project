package views.admin.submissions

import shared.endpoints.GetFormSubmission
import com.raquo.laminar.api.L._
import com.github.uosis.laminar.webcomponents.material
import dao.forms._
import shared.utils.DateFormatting
import scala.scalajs.js.Date
import com.github.uosis.laminar.webcomponents.material.Icon
import com.raquo.laminar.keys.ReactiveProp
import com.raquo.domtypes.generic.codecs.StringAsIsCodec

object SubmissionUtils {

  val graphic = new ReactiveProp("graphic", StringAsIsCodec)

  def determineIcon(formSubmissionStatus: FormSubmissionStatus) =
    formSubmissionStatus match {
      case Ready()                                   => span("pending")
      case Submitted(_submittedById, submissionDate) => span("inbox")
      case InReview(
            _submittedById,
            _reviewerId,
            submissionDate,
            reviewStartDate
          ) =>
        span("group")
      case Reviewed(
            _submittedById,
            _reviewerId,
            submissionDate,
            reviewStartDate,
            reviewEndDate,
            changesRequested,
            notes
          ) =>
        span("grading")
    }

  def submissionListItemFactory(formSubmission: GetFormSubmission) =
    material
      .List
      .ListItem(
        _.twoline(true),
        _.multipleGraphics(true),
        _.slots.graphic(Icon(_.slots.default(determineIcon(formSubmission.submission.formSubmissionStatus)))),
        _.slots.default(
          span(
            span(formSubmission.formResource.details.name, marginRight("0.25rem")),
            span(
              overflow.hidden,
              textOverflow.ellipsis,
              color := "var(--mdc-theme-text-secondary-on-background)",
              s"#${formSubmission.submission._id}"
            )
          )
        ),
        _.slots.secondary(
          span(formSubmission.submission.formSubmissionStatus match {
            case Ready() => ""
            case Submitted(_submittedById, submissionDate) =>
              DateFormatting.longToString(submissionDate)
            case InReview(
                  _submittedById,
                  _reviewerId,
                  submissionDate,
                  reviewStartDate
                ) =>
              DateFormatting.longToString(reviewStartDate)
            case Reviewed(
                  _submittedById,
                  _reviewerId,
                  submissionDate,
                  reviewStartDate,
                  reviewEndDate,
                  changesRequested,
                  notes
                ) =>
              DateFormatting.longToString(reviewEndDate)
          })
        )
      )
      .amend(graphic("avatar"))

}
