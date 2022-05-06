package components.list

import components.Material
import com.raquo.laminar.api.L._
import components.list.item.MaterialListItem
import scala.collection.immutable

class FilteredList[ItemType <: MaterialListItem](
    materialList: MaterialList[ItemType]
) extends MaterialList[ItemType] {
  val originalItems: Var[Seq[ItemType]]        = materialList.listItems
  val filtersVar: Var[Seq[ItemType => Boolean]] = Var(Seq())
  override val listItems: Var[Seq[ItemType]]            = Var(materialList.listItems.now())

  override def render(): HtmlElement =
    super
      .render()
      .amend(
        materialList.listItems --> originalItems,
        originalItems --> listItems,
        listItems --> { _ => filter },
        filtersVar --> { filters =>
          filtersVar.set(filters)
          listItems
            .set(filters.foldRight(originalItems.now())((_, _) => filter))
        }
      )

  def filter = filtersVar
    .now()
    .flatMap(originalItems.now.filter(_))
    .toList
}
