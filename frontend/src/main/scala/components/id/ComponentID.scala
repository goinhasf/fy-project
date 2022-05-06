package components.id

import components.MaterialComponent
import java.util.UUID

case class ComponentID(componentName: String = "mdc-component") {
  val id: UUID                    = UUID.randomUUID()
  override def toString(): String = componentName + "#" + id.toString()
}
