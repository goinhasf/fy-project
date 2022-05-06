package shared.auth

import io.circe.generic.JsonCodec
@JsonCodec
case class Token(
    _userId: String,
    encodedValue: String,
    _id: String
)
