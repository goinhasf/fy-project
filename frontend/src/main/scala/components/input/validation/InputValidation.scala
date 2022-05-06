package components.input.validation

import components.input.InputError

trait InputValidationComponent[A] {
  val inputValidation: InputValidation[A]
}

trait InputValidation[A] {
  def validate(value: A): Either[InputError, A]
}

case class NoValidation[A]() extends InputValidation[A] {
  def validate(value: A): Either[InputError, A] = Right(value)
}

case class Required[T](isEmpty: T => Boolean) extends InputValidation[T] {
  override def validate(value: T): Either[InputError, T] = {
    if (isEmpty(value)) Left(InputError("Value is required"))
    else Right(value)
  }
}
