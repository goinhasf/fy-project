package dao.events

import io.circe.generic.JsonCodec

@JsonCodec
case class SocietyEventType(
    name: String
)
