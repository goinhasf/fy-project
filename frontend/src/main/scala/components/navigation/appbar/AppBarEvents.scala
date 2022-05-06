package components.navigation.appbar

sealed trait AppBarEvents
case class PrimaryButtonClicked(buttonType: ButtonType) extends AppBarEvents
case class ActionButtonClicked(
    actionButton: AppBarActionButton
) extends AppBarEvents

sealed trait ButtonType
case object BackButton extends ButtonType
case object MenuButton extends ButtonType
