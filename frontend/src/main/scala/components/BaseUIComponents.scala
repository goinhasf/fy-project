package components

import components.navigation.appbar.TopAppBar
import components.navigation.drawer.NavigationDrawer
import components.button.FloatingActionButton
import views.ui.AppNavigationDrawer
import com.raquo.laminar.api.L._
import org.scalajs.dom

case class BaseUIComponents(
    topAppBar: TopAppBar,
    drawer: AppNavigationDrawer,
    fab: FloatingActionButton
) {
  val eventBus = new EventBus[DisplayUIEvent]
  topAppBar.amend(
    display <-- eventBus
      .events
      .collect({ case e: DisplayTopAppBarEvent => e.setter })
  )
  fab.editRoot(el =>
    eventBus
      .events
      .collect({ case e: DisplayFabEvent => e.show }) --> { show =>
      if (show) el.show() else el.hide()
    }
  )
}

sealed trait DisplayUIEvent
case class DisplayTopAppBarEvent(show: Boolean) extends DisplayUIEvent {
  val setter: String = if (show) "flex" else "none"
}
case class DisplayFabEvent(show: Boolean) extends DisplayUIEvent
