import scala.concurrent.Future

import com.raquo.laminar.api.L._
import org.scalajs.dom

import router.ApplicationRouter


object Main {
  def main(args: Array[String]): Unit = {
    ApplicationRouter.loadApp()
  }
}
