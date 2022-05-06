package components.style

import com.raquo.laminar.api.L.Var

trait ComponentStyle {
  val initialStyles: Seq[String]
  lazy val stylesMap: Var[Map[String, Boolean]] = Var(
    initialStyles
      .foldRight(Map.empty[String, Boolean])((a, b) => b + (a -> true))
  )

}
object ComponentStyle {
  def apply(style: Seq[String]): ComponentStyle = new ComponentStyle {
    override val initialStyles = style
  }
}
