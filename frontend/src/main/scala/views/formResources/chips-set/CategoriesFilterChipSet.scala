package views.formResources.chipSet

import components.chip.FilterChipSet
import dao.forms.FormCategory
import com.raquo.laminar.api.L._
import components.chip.Chip
import components.chip.FilterChip
import components.chip.ChipIconLeading
import components.chip.ChipSelected

class CategoriesFilterChipSet extends FilterChipSet {

  def setCategories(formCategories: Set[FormCategory]): Unit = {
    chips.set(formCategories.map(CategoryFilterChip(_)))
  }

  def addCategory(formCategory: FormCategory): Unit = {
    chips.now().find(_.valueVar.now() == formCategory.categoryName) match {
      case Some(_) => {}
      case None    => addChip(CategoryFilterChip(formCategory))
    }
  }

}
case class CategoryFilterChip(category: FormCategory)
    extends FilterChip(
      category.categoryName,
      leadingIcon = Some(new ChipIconLeading("cross"))
    )
