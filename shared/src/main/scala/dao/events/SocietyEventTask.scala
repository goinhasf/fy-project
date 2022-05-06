package dao.events

import io.circe.generic.JsonCodec

@JsonCodec
case class SocietyEventTask(
    _id: String,
    _eventId: String,
    details: SocietyEventTaskDetails
)

@JsonCodec
case class SocietyEventTaskDetails(
    taskBearerId: String,
    taskName: String,
    taskDescription: String,
    taskDueTime: Long,
    isDone: Boolean
)