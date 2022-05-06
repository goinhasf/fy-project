package components.form

import components.input.InputError

sealed trait FormEvents[Result]
case class FormInputErrorEvent[Result](error: InputError) extends FormEvents[Result]
case class FormSubmitted[Result]() extends FormEvents[Result]
case class FormResultReceived[Result](result: Result) extends FormEvents[Result]