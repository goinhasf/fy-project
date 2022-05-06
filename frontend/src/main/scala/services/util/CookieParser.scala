package services.util

object CookieParser {
  def parseCookies(cookieString: String): Map[String, String] = {
    val arr = cookieString
      .split(";")
      .map(_.replace(" ", ""))

    arr.foldRight(Map.empty[String, String])((a, b) => {
      val splitKeyValue = a.split("=")
      if (splitKeyValue.length == 2) {
        b + (splitKeyValue(0) -> splitKeyValue(1))
      } else {
        b
      }
    })
  }
}
