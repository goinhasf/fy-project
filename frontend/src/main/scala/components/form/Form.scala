package components.form

import scala.util.Random
import org.scalajs.dom.raw.FormData
import org.scalajs.dom.raw.XMLHttpRequest
import org.scalajs.dom.raw.Event
import scala.concurrent.Promise
import scala.concurrent.Future
import org.scalajs.dom.experimental.HttpMethod
import org.scalajs.dom.raw.File
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
trait Form {
  val fileParts: Seq[FormPart[_]]
  lazy val formData: FormData = {
    val data = new FormData()
    fileParts.foreach(part =>
      part match {
        case FileFormPart(name, value) => data.append(name, value)
        case json: JsonFormPart        => data.append(json.name, json.value)
        case _ =>
          throw new IllegalArgumentException("Form Part kind not supported")
      }
    )
    data
  }
}
object Form {
  def apply(parts: FormPart[_]*): Form = new Form {
    override val fileParts: Seq[FormPart[_]] = parts.toSeq
  }

  def apply(parts: List[FormPart[_]]): Form = new Form {
    override val fileParts: Seq[FormPart[_]] = parts.toSeq
  }
}
trait FormPart[A] {
  val name: String
  val value: A
}

case class JsonFormPart(name: String, mValue: Json) extends FormPart[String] {
  override val value: String = mValue.noSpacesSortKeys
}

case class FileFormPart(
    override val name: String,
    override val value: File
) extends FormPart[File]
