package views.formResourceDetails

import components.input.MaterialInput
import components.MDCComponent
import org.scalajs.dom.html
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.github.uosis.laminar.webcomponents.material
import com.github.uosis.laminar.webcomponents.material

case class SelectComponent(
    label: String,
    seq: Seq[String],
    defaultValue: String
) extends MaterialInput[String, MDCComponent, html.Select] {

  private val valueVar: Var[String] = Var(seq.headOption.getOrElse(""))

  private val listItems = seq.map(key =>
    material
      .List
      .ListItem(
        _.selected(defaultValue == key),
        _.value(key),
        _.slots.default(span(key))
      )
  )

  private val selectElement = listItems
    .foldLeft(
      material
        .Select(
          _.outlined(true),
          _.label(label),
          _.value <-- valueVar,
          _.slots.default(material.List.ListItem(_.value := ""))
        )
    )((sel, item) => sel.amend(material.Select.slots.default(item)))
    .amend(onChange.mapToValue --> valueVar)

  override protected val rootElement: HtmlElement = selectElement

  override def getInputElement(): ReactiveHtmlElement[html.Select] =
    selectElement
      .ref
      .asInstanceOf[Select]

  override def getValue(): Var[String] = valueVar

  override def clearInput(): Unit = {
    selectElement.amend(material.Select.value := "")
  }

}
