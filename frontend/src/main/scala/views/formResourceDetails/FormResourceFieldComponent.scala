package views.formResourceDetails

import dao.forms.FormResourceFieldDescriptor
import components.input.text.OutlinedTextField
import io.circe.Json
import io.circe.HCursor
import io.circe.ACursor
import components.input.MaterialInput
import org.scalajs.dom.html
import components.MDCComponent
import dao.forms.FormResource
import com.raquo.laminar.api.L._
import components.button.MaterialButton
import components.button.label.TextButtonLabel
import components.button.style.ButtonStyles
import components.list.MaterialList
import components.list.item.MaterialListItem
import components.list.item.builders.EmptyBeforeListElement
import components.list.item.builders.EmptyAfterListElement
import components.list.item.builders.SingleLineTextElement
import components.list.item.builders.AfterListItemIcon
import components.Material
import components.id.Identifiable
import components.id.ComponentID
import io.circe.JsonObject
import io.circe.syntax._
import io.circe._
import components.MaterialComponent
import dao.forms.SingleFormResourceFieldDescriptor
import dao.forms.LoopFormResourceFieldDescriptor
import scala.collection.immutable
import components.list.item.builders.ListItemTextElement
import com.github.uosis.laminar.webcomponents.material._
import com.github.uosis.laminar.webcomponents.material
import dao.events.EventWizardQuestionResolver
import scala.util.Try

// TODO: Populate answers
sealed trait FormResourceFieldComponent
    extends MaterialComponent[MDCComponent]
    with Identifiable {
  val id: ComponentID            = ComponentID("mdc-form-resource-field")
  val isEditingVar: Var[Boolean] = Var(false)
  val state: Var[JsonObject]     = Var(JsonObject.empty)
  val formResourceField: FormResourceFieldDescriptor
  val answers: Option[JsonObject]
  def toJsonObject: JsonObject
}

object FormResourceFieldComponent {

  def createInputsFromJsonObject(
      descriptors: Seq[FormResourceFieldDescriptor],
      answer: Option[JsonObject]
  ): Seq[FormResourceFieldComponent] = descriptors
    .map(_ match {
      case s: SingleFormResourceFieldDescriptor =>
        new SingleFormResourceFieldComponent(
          s,
          answer
        )
      case l: LoopFormResourceFieldDescriptor =>
        new LoopFormResourceFieldComponent(l, answer)
    })

  def getJsonObjectFromFields(seq: Seq[FormResourceFieldComponent]) = seq
    .map(_.toJsonObject)
    .foldRight(JsonObject.empty)(_ deepMerge _)
}

case class SingleFormResourceFieldComponent(
    override val formResourceField: SingleFormResourceFieldDescriptor,
    override val answers: Option[JsonObject] = None,
    isEditing: Boolean = true
) extends FormResourceFieldComponent {

  override val isEditingVar: Var[Boolean] = Var(isEditing)

  override val state: Var[JsonObject] = Var(answers.getOrElse(JsonObject.empty))

  private def getSelectOptions = formResourceField
    .acceptedValues
    .map(keys =>
      SelectComponent(
        label,
        keys,
        getValueFromAnswer.getOrElse("")
      ).editRoot { el =>
        onChange.mapTo(toJsonObject) --> state
      }
    )

  val label: String = formResourceField
    .path
    .flatMap(_.split('_'))
    .map(FormResourceFieldDescriptor.firstLetterToUpperCase)
    .reduceRight((a, b) => s"$a $b")

  def getValueFromAnswer: Option[String] = {
    for {
      jsonAns <- answers.map(_.asJson)
      typ     <- formResourceField.getType.toOption
      cursor = formResourceField
        .path
        .foldLeft[ACursor](jsonAns.hcursor)((cursor, key) =>
          cursor.downField(key)
        )
      result <- typ match {
        case "string" => cursor.as[String].toOption
        case "number" => cursor.as[Long].map(_.toString()).toOption
        case _        => cursor.as[String].toOption
      }
    } yield result
  }

  private val textField = new OutlinedTextField(label)
    .editRoot { el =>
      el.getInputElement()
        .amend(typ(formResourceField.getType match {
          case Left(value)  => "text"
          case Right(value) => value
        }))
      el.getValue.set(getValueFromAnswer.getOrElse(""))
      el.floatLabel()
      el.textFieldContent
        .inputElement
        .amend(
          disabled <-- isEditingVar.signal.map(!_),
          onChange.mapTo(toJsonObject) --> state
        )
      isEditingVar.signal.map(!_) --> { disabled =>
        el.fromFoundation(_.disabled = disabled)
      }
    }
    .editRoot(display.flex)

  val input: MaterialInput[String, _ <: MDCComponent, html.Element] =
    getSelectOptions match {
      case Some(value) => value
      case None        => textField
    }

  protected val rootElement: HtmlElement = input

  def toJsonObject: JsonObject = {
    def determineValue = formResourceField
      .getType
      .getOrElse("string") match {
      case "number" =>
        Try(input.getValue().now().toDouble.asJson)
          .toOption
          .orElse(Some("".asJson))
      case _ => Some(input.getValue().now().asJson)

    }
    formResourceField.descriptorToJsonObject(
      determineValue
    )
  }

}

case class LoopFormResourceFieldComponent(
    override val formResourceField: LoopFormResourceFieldDescriptor,
    override val answers: Option[JsonObject] = None,
    isEditing: Boolean = true
) extends FormResourceFieldComponent {

  override val isEditingVar: Var[Boolean] = Var(isEditing)

  val (rootKey, paths) = {
    val rootKey = formResourceField.paths.head.head
    (rootKey, formResourceField.paths.map(_.tail))
  }

  def getValuesFromAnswers = for {
    ans <- answers.map(_.asJson)
    arr <- ans.asJson.hcursor.downField(rootKey).as[Array[Json]].toOption
  } yield makeMapFromArr(arr)

  def makeMapFromArr(arr: Array[Json]) = {
    def tailRecHelper(
        cursor: ACursor,
        acc: Seq[String]
    ): (Seq[String], String) = {
      val cursorKeys = cursor.keys.toSeq.flatten
      if (cursorKeys.length == 0) {
        (acc, cursor.as[String].getOrElse(""))
      } else {
        tailRecHelper(
          cursor.downField(cursorKeys.head),
          acc :+ cursorKeys.head
        )
      }
    }
    def buildEntry(json: Json) = tailRecHelper(json.hcursor, Seq())

    def allKeysAsJson(json: Json) = json
      .hcursor
      .keys
      .toSeq
      .flatten
      .map { key =>
        JsonObject(
          key -> json
            .hcursor
            .downField(key)
            .as[Json]
            .getOrElse(JsonObject.empty.asJson)
        ).asJson
      }

    arr
      .map(allKeysAsJson)
      .map(jsons => { println(jsons); jsons.map(buildEntry) })
      .map(_.toMap)
      .toSeq

  }

  case class Item(map: Map[Seq[String], String])
      extends MaterialListItem
      with EmptyBeforeListElement
      with AfterListItemIcon
      with ListItemTextElement {

    def textElement: HtmlElement = {
      val capitalized = map
        .map(entry =>
          (
            entry
              ._1
              .map(FormResourceFieldDescriptor.firstLetterToUpperCase)
              .reduceRight((a, b) => s"$a $b"),
            entry._2
          )
        )
      capitalized
        .foldLeft[HtmlElement](div())((el, entry) =>
          el.amend(
            div(
              b(
                marginRight := "0.25rem",
                s"${entry._1}:"
              ),
              span(marginRight := "0.25rem", entry._2)
            )
          )
        )
        .amend(padding := "0.5rem")
    }

    val afterListItemIcon: Var[String] = Var("highlight_off")

  }
  val inputTextFields = paths
    .map(key =>
      (
        key,
        new OutlinedTextField(
          key
            .map(FormResourceFieldDescriptor.firstLetterToUpperCase)
            .reduceRight((a, b) => s"$a $b")
        ).editRoot { el =>
          el.floatLabel()
          el.textFieldContent
            .inputElement
            .amend(disabled <-- isEditingVar.signal.map(!_))
          isEditingVar.signal.map(!_) --> { disabled =>
            el.fromFoundation(_.disabled = disabled)
          }
        }
      )
    )
    .toMap

  val listOfValues = new MaterialList[Item]() {
    this
      .listItems
      .set(getValuesFromAnswers.map(_.map(Item)).getOrElse(Seq()).map { el =>
        addElement(el)
        el
      })

    override def addElement(e: Item): Unit = {
      super.addElement(
        e.editRoot(
          _.afterListElement
            .events(onClick.mapTo(e.id)) --> { id =>
            listItems.update(
              _.filter(item => item.id.toString != id.toString())
            )
          }
        ).editRoot(height.auto)
      )
    }
  }.editRoot(_.listItems.signal.mapTo(toJsonObject) --> state)

  protected val rootElement: HtmlElement = div(
    h3(FormResourceFieldDescriptor.firstLetterToUpperCase(rootKey)),
    div(
      padding := "0.5rem",
      inputTextFields
        .toSeq
        .map(_._2)
        .map(field => div(marginBottom := "1rem", field))
    ),
    MaterialButton(TextButtonLabel("Add"), ButtonStyles.raisedButtonStyle)
      .editRoot(
        onClick.mapTo(
          inputTextFields.map(entry => (entry._1, entry._2.getValue().now()))
        ) --> { value =>
          val item = Item(value)
          listOfValues.addElement(item)
        }
      ),
    listOfValues
  )

  def toJsonObject: JsonObject = {
    JsonObject(
      rootKey -> listOfValues
        .listItems
        .now()
        .map(_.map)
        .map(_.map(entry => (entry._1, entry._2.asJson)))
        .map(formResourceField.descriptorToJsonObject)
        .flatMap(_.apply(rootKey).toSeq)
        .asJson
    )
  }

  def editTextFields(mod: OutlinedTextField => Mod[HtmlElement]): this.type = {
    inputTextFields.values.map(_.editRoot(mod))
    this
  }

}
