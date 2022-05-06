package components.list

import com.raquo.laminar.api.L._
import components.MDCList
import components.MaterialComponent
import components.style.ComponentStyle
import components.list.item.MaterialListItem
import org.scalajs.dom
import components.id.Identifiable
import components.id.ComponentID
import com.raquo.laminar.builders.HtmlTag

class MaterialList[ItemType <: MaterialListItem](
    initialItems: Seq[ItemType] = List()
) extends MaterialComponent[MDCList]
    with Identifiable {

  val listItems: Var[Seq[ItemType]] = Var(initialItems)
  val id: ComponentID               = ComponentID("mdc-list")

  protected lazy val rootElement: HtmlElement = {
    ul(
      idAttr := id.toString(),
      onMountCallback(_ =>
        mdcComponent.set(
          Some(new MDCList(dom.document.getElementById(id.toString())))
        )
      ),
      cls := "mdc-list",
      children <-- listItems
        .signal
        .split(_.id.toString())((_, item, _) => item.render()),
      initialItems
        .headOption
        .map(
          _.editRoot(_.amendThis(el => onClick --> (_ => el.ref.focus())))
        )
    )
  }

  def addElement(e: ItemType): Unit = {
    listItems.update(current => e +: current)
  }

}
