package components.navigation.drawer

sealed trait NavigationDrawerEvents
sealed case class DrawerStateChange(state: Boolean)
    extends NavigationDrawerEvents
sealed case class ToggleState() extends NavigationDrawerEvents
