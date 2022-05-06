package shared.utils

import java.util.Date
import java.text.SimpleDateFormat

object DateFormatting {

  val DATE_FORMAT    = "YYYY-MM-DD"
  val TIME_FORMAT    = "HH:mm"
  val combinedFormat = TIME_FORMAT + ", " + DATE_FORMAT

  def getDateAsString(d: Date): String = {
    val dateFormat = new SimpleDateFormat(combinedFormat)
    dateFormat.format(d)
  }

  def convertStringToDate(date: String, time: String): Date = {
    val dateFormat = new SimpleDateFormat(combinedFormat)
    dateFormat.parse(s"$time, $date")
  }
}
