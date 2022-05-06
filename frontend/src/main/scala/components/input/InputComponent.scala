package components.input
import org.scalajs.dom.html
import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import components.Material
import components.MaterialComponent
import components.MDCComponent

trait MaterialInput[T, M <: MDCComponent, +Ref <: html.Element]
    extends MaterialComponent[M]
    with InputComponent[T, Ref]

trait InputComponent[T, +Ref <: html.Element] {
  def getInputElement(): ReactiveHtmlElement[Ref]
  def getValue(): Var[T]
  def clearInput(): Unit
}

