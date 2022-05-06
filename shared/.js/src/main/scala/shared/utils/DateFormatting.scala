package shared.utils

import scala.scalajs.js.Date
import moment._

object DateFormatting {

  val DATE_FORMAT    = "YYYY-MM-DD"
  val TIME_FORMAT    = "HH:mm"
  val combinedFormat = TIME_FORMAT + ", " + DATE_FORMAT

  def getDateAsString(d: Date): String = {
    d.formatted(combinedFormat)
  }

  def convertStringToDate(date: String, time: String): Date = {
    new Date(Moment(s"$time, $date").format(combinedFormat))
  }

  def longToString(long: Long): String = {
    Moment(long).format(combinedFormat)
  }
}
