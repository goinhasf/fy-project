package dao.forms

import io.circe.JsonObject
import io.circe.generic.JsonCodec
import io.circe.Json
import io.circe.ACursor
import io.circe._
import io.circe.parser._
import io.circe.syntax._
import scala.collection.immutable
import io.circe.generic.extras.ConfiguredJsonCodec

sealed trait FormResourceFieldDescriptor {
  val cardinality: String
  val field: Json
  val acceptedValues: Option[Seq[String]] = None
  def descriptorToJsonObject: JsonObject
}
object FormResourceFieldDescriptor {
  implicit val codec = new Codec[FormResourceFieldDescriptor] {
    def apply(c: HCursor): Decoder.Result[FormResourceFieldDescriptor] = for {
      cardinality    <- c.downField("cardinality").as[String]
      field          <- c.downField("field").as[Json]
      acceptedValues <- c.downField("acceptedValues").as[Option[Seq[String]]]
    } yield cardinality match {
      case "single" => SingleFormResourceFieldDescriptor(field, acceptedValues)
      case "loop"   => LoopFormResourceFieldDescriptor(field)
    }
    def apply(a: FormResourceFieldDescriptor): Json = Json.obj(
      "cardinality"    -> a.cardinality.asJson,
      "field"          -> a.field,
      "acceptedValues" -> a.acceptedValues.asJson
    )
  }

  def matchType(typeString: String)(value: String): Json = typeString match {
    case "number" => value.toDouble.asJson
    case _        => value.asJson
  }

  def firstLetterToUpperCase(s: String) = {
    s.substring(0, 1).toUpperCase() + s.substring(1)
  }

}

@JsonCodec
case class SingleFormResourceFieldDescriptor(
    override val field: Json,
    override val acceptedValues: Option[Seq[String]] = None
) extends FormResourceFieldDescriptor {

  def path: Seq[String] = {
    def tailRecHelper(cursor: ACursor, acc: Seq[String]): Seq[String] = {
      val cursorKeys = cursor.keys.toSeq.flatten
      if (cursorKeys.length == 0) {
        acc
      } else {
        tailRecHelper(cursor.downField(cursorKeys.head), acc :+ cursorKeys.head)
      }
    }
    tailRecHelper(field.hcursor, Seq())
  }

  def getType = path
    .foldLeft[ACursor](field.hcursor)((cursor, key) => cursor.downField(key))
    .as[String]

  override val cardinality: String = "single"

  override def descriptorToJsonObject: JsonObject = descriptorToJsonObject()
  def descriptorToJsonObject(
      defaultLeafVal: Option[Json] = None
  ): JsonObject = {

    val keys    = field.hcursor.keys.toSeq.flatten
    val rootKey = keys.head

    def tailRecHelper(cursor: ACursor): Json = {
      val cursorKeys = cursor.keys.toSeq.flatten
      if (cursorKeys.length == 0) {
        defaultLeafVal.getOrElse(cursor.as[Json].getOrElse("".asJson))
      } else {
        cursorKeys
          .foldRight(JsonObject.empty)((key, jsObject) =>
            jsObject
              .add(key, tailRecHelper(cursor.downField(key)))
          )
          .asJson
      }
    }
    JsonObject(
      rootKey -> tailRecHelper(field.hcursor.downField(rootKey))
    )
  }

}
@JsonCodec
case class LoopFormResourceFieldDescriptor(override val field: Json)
    extends FormResourceFieldDescriptor {

  val cardinality: String = "loop"

  def paths: Seq[Seq[String]] = {
    val rootKey = field.hcursor.keys.toSeq.flatten.head
    field
      .hcursor
      .downField(rootKey)
      .as[Seq[FormResourceFieldDescriptor]]
      .getOrElse(Seq())
      .map(_ match {
        case s: SingleFormResourceFieldDescriptor => rootKey +: s.path
        case l: LoopFormResourceFieldDescriptor =>
          l.paths.flatMap(s => rootKey +: s)
      })
  }

  def singlePath: Seq[String] = {
    def tailRecHelper(cursor: ACursor, acc: Seq[String]): Seq[String] = {
      val cursorKeys = cursor.keys.toSeq.flatten
      if (cursorKeys.length == 0) {
        acc
      } else {
        tailRecHelper(cursor.downField(cursorKeys.head), acc :+ cursorKeys.head)
      }
    }
    tailRecHelper(field.hcursor, Seq())
  }
  override def descriptorToJsonObject: JsonObject = descriptorToJsonObject()

  def descriptorToJsonObject(
      defaultLeafVal: Map[Seq[String], Json] = Map()
  ): JsonObject = {
    val rootKey = field.hcursor.keys.toSeq.flatten.head
    JsonObject(
      rootKey -> field
        .hcursor
        .downField(rootKey)
        .as[Seq[FormResourceFieldDescriptor]]
        .getOrElse(Seq())
        .map(_ match {
          case s: SingleFormResourceFieldDescriptor =>
            s.descriptorToJsonObject(defaultLeafVal.get(s.path))
          case l: LoopFormResourceFieldDescriptor => l.descriptorToJsonObject
        })
        .fold(JsonObject.empty)(_ deepMerge _)
        .asJson
    )
  }
}

object LoopFormResourceFieldDescriptor {
  def singlePath(json: Json): Seq[String] = {
    def tailRecHelper(cursor: ACursor, acc: Seq[String]): Seq[String] = {
      val cursorKeys = cursor.keys.toSeq.flatten
      if (cursorKeys.length == 0) {
        acc
      } else {
        tailRecHelper(cursor.downField(cursorKeys.head), acc :+ cursorKeys.head)
      }
    }
    tailRecHelper(json.hcursor, Seq())
  }
}

@JsonCodec
case class FormResourceFields(
    _id: String,
    _formResourceId: String,
    descriptors: Seq[FormResourceFieldDescriptor]
) {
  def descriptorsToJsonObject: JsonObject =
    descriptors
      .map(_.descriptorToJsonObject)
      .fold(JsonObject.empty)(_ deepMerge _)
}
