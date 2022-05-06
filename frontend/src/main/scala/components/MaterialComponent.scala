package components

import com.raquo.domtypes.generic.codecs.StringAsIsCodec
import com.raquo.laminar.builders.HtmlTag
import org.scalajs.dom
import com.raquo.laminar.api.L._

trait Material {
  def render(): HtmlElement
}

trait MaterialComponent[T <: MDCComponent]
    extends MaterialComponent.MaterialComponentAttributes
    with Material {
  protected val rootElement: HtmlElement
  val mdcComponent: Var[Option[T]] = Var(None)
  def render(): HtmlElement        = rootElement
  def editRoot(mods: Mod[HtmlElement]*): this.type = {
    rootElement.amend(mods)
    this
  }
  def editRoot(mods: this.type => Mod[HtmlElement]): this.type = {
    rootElement.amend(mods(this))
    this
  }

  def fromFoundation[A](f: T => A): this.type = {
    mdcComponent.now().foreach(f)
    this
  }

}
object MaterialComponent {
  trait MaterialComponentAttributes {
    val ariaLabel     = new HtmlAttr[String]("aria-label", StringAsIsCodec)
    val ariaLabeledBy = new HtmlAttr[String]("aria-labelledby", StringAsIsCodec)
    val ariaHidden    = new HtmlAttr[String]("aria-hidden", StringAsIsCodec)
    val ariaModal     = new HtmlAttr[String]("aria-modal", StringAsIsCodec)
    val ariaDescribedBy =
      new HtmlAttr[String]("aria-describedby", StringAsIsCodec)
    val role = new HtmlAttr[String]("role", StringAsIsCodec)
    val mdcAutoInit =
      new HtmlAttr[String]("data-mdc-auto-init", StringAsIsCodec)
    val ariaCurrent =
      new HtmlAttr[String]("aria-current", StringAsIsCodec)

    val tabIndex =
      new HtmlAttr[String]("tabindex", StringAsIsCodec)
  }

  @inline implicit def toHtmlElement[T <: MDCComponent](
      c: MaterialComponent[T]
  ): HtmlElement =
    c.render()

  @inline implicit def toHtmlElement(
      c: Material
  ): HtmlElement =
    c.render()
}
