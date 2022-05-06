package shared.auth

import io.circe.generic.JsonCodec

@JsonCodec
case class ApiKey(
    serviceId: String,
    key: String
)
