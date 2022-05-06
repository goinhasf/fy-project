package components.form.chip

import components.input.chip.InputChipSet
import components.input.validation.InputValidation
import components.input.validation.NoValidation
import components.form.FormField
import com.raquo.airstream.eventbus.EventBus
import components.form.FormEvents

case class InputChipFormField[R](
    name: String,
    inputChipSet: InputChipSet,
    validation: InputValidation[List[String]] = NoValidation()
)(implicit formEventBus: EventBus[FormEvents[R]])
    extends FormField[List[String], R](name, inputChipSet, validation)
