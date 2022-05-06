package components.ripple
import com.raquo.laminar.api.L._
import components.MDCRipple
import org.scalajs.dom
import components.MaterialComponent
import components.id.Identifiable
import components.MDCComponent
import components.id.ComponentID

trait Ripple { self: Identifiable =>
  def ripple: HtmlElement = span(
    cls := s"${self.id.componentName}__ripple",
    onMountCallback(ctx => {
      new MDCRipple(dom.document.getElementById(self.id.toString()))
    })
  )
}
object Ripple {
  def apply[T <: MDCComponent](mId: Identifiable): HtmlElement = span(
    cls := s"${mId.id.componentName}__ripple",
    onMountCallback(ctx => {
      new MDCRipple(dom.document.getElementById(mId.id.toString()))
    })
  )

  def apply[T <: MDCComponent](mId: ComponentID): HtmlElement = span(
    cls := s"${mId.componentName}__ripple",
    onMountCallback(ctx => {
      new MDCRipple(dom.document.getElementById(mId.toString()))
    })
  )
}