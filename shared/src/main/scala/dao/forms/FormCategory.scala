package dao.forms

import io.circe.generic.JsonCodec

@JsonCodec
case class FormCategory(categoryName: String)

