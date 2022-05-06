package components

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import org.scalajs.dom.raw.Node
import org.scalajs.dom.raw.Element

@js.native
@JSImport("@material/auto-init", JSImport.Default)
object AutoInit extends js.Object {
  def apply(document: Node): Unit = js.native
  def apply(): Unit               = js.native
  def register[T <: js.Any](
      fieldName: js.JSStringOps,
      component: js.Dynamic
  ): Nothing = js.native
}
@js.native
@JSImport("@material/base", "MDCComponent")
class MDCComponent(component: org.scalajs.dom.Node) extends js.Object {
  def initialize(args: js.Array[Any]): Unit = js.native
  def getDefaultFoundation(): js.Object     = js.native
  def destroy(): Unit                       = js.native
  def listen[E](
      eventType: String,
      handler: js.Function1[E, Unit],
      options: Option[js.Object]
  ): Unit = js.native
  def unlisten[E](
      eventType: String,
      handler: js.Function1[E, Unit],
      options: Option[js.Object]
  ): Unit = js.native
  def emit[T](
      eventType: String,
      eventData: T,
      shouldBubble: Boolean = false
  ): Unit = js.native
}

@js.native
@JSImport("@material/base", "MDCFoundation")
class MDCFoundation[T <: js.Object](adapter: T) extends js.Object {
  def cssClasses(): Map[String, String] = js.native
  def strings(): Map[String, String]    = js.native
  def numbers(): Map[String, Int]       = js.native
  def defaultAdapter(): js.Object       = js.native
  def init(): Unit                      = js.native
  def destroy(): Unit                   = js.native
}

@js.native
@JSImport("@material/ripple", "MDCRipple")
class MDCRipple(component: org.scalajs.dom.Node) extends MDCComponent(component)

@js.native
@JSImport("@material/ripple", "MDCButton")
class MDCButton(component: org.scalajs.dom.Element)
    extends MDCComponent(component)

@js.native
@JSImport("@material/drawer", "MDCDrawer")
class MDCDrawer(component: org.scalajs.dom.Element)
    extends MDCComponent(component) {
  var open: Boolean = js.native
}
@js.native
@JSImport("@material/drawer", "MDCDrawer")
object MDCDrawer extends js.Object {
  def attachTo(element: org.scalajs.dom.Element): MDCDrawer = js.native
}
@js.native
@JSImport("@material/top-app-bar", "MDCTopAppBar")
class MDCTopAppBar(component: org.scalajs.dom.Element)
    extends MDCComponent(component)

@js.native
@JSImport("@material/textfield", "MDCTextField")
class MDCTextField(component: org.scalajs.dom.Element)
    extends MDCComponent(component) {
  var valid: Boolean               = js.native
  var disabled: Boolean            = js.native
  var useNativeValidation: Boolean = js.native
  var required: Boolean            = js.native
  def focus(): Unit                = js.native
}

@js.native
@JSImport("@material/list", "MDCList")
class MDCList(component: org.scalajs.dom.Element)
    extends MDCComponent(component) {
  var singleSelection: Boolean                             = js.native
  def setEnabled(itemIndex: Int, isEnabled: Boolean): Unit = js.native
  def initializeListType(): Unit                           = js.native
}

@js.native
@JSImport("@material/dialog", "MDCDialog")
class MDCDialog(component: org.scalajs.dom.Element)
    extends MDCComponent(component) {
  def open(): Unit                = js.native
  def close(action: String): Unit = js.native
}

@js.native
@JSImport("@material/chips", "MDCChip")
class MDCChip(component: org.scalajs.dom.Element)
    extends MDCComponent(component) {
  var selected: Boolean                               = js.native
  def beginExit(): Unit                               = js.native
  def setSelectedFromChipSet(selected: Boolean): Unit = js.native
}

@js.native
@JSImport("@material/chips", "MDCChipSet")
class MDCChipSet(component: org.scalajs.dom.Element)
    extends MDCComponent(component) {
  def addChip(chipEl: Element): Unit    = js.native
  var chips: js.Array[MDCChip]          = js.native
  var selectedChipIds: js.Array[String] = js.native
}

@js.native
@JSImport("@material/switch", "MDCSwitch")
class MDCSwitch(component: org.scalajs.dom.Element)
    extends MDCComponent(component) {
  var checked: Boolean  = js.native
  var disabled: Boolean = js.native
}
@js.native
@JSImport("@material/menu", "MDCMenu")
class MDCMenu(component: org.scalajs.dom.Element)
    extends MDCComponent(component) {
  var open: Boolean                            = js.native
  var items: Array[Element]                    = js.native
  def setFixedPosition(isFixed: Boolean): Unit = js.native
  def setAnchorElement(element: Element): Unit = js.native

}
@js.native
@JSImport("@material/radio", "MDCRadio")
class MDCRadio(component: org.scalajs.dom.Element)
    extends MDCComponent(component) {}
