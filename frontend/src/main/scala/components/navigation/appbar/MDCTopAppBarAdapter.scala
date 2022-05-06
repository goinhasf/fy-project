package components.navigation.appbar

trait MDCTopAppBarAdapter {

  /** Adds a class to the root Element.
    */
  def addClass(className: String): Unit;

  /** Removes a class from the root Element.
    */
  def removeClass(className: String): Unit;

  /** Returns true if the root Element contains the given class.
    */
  def hasClass(className: String): Boolean;

  /** Sets the specified inline style property on the root Element to the given value.
    */
  def setStyle(property: String, value: String): Unit;

  /** Gets the height of the top app bar.
    */
  def getTopAppBarHeight(): Int;
  def getViewportScrollY(): Int;
  def getTotalActionItems(): Int;

  /** Emits an event when the navigation icon is clicked.
    */
  def notifyNavigationIconClicked(): Unit;
}
