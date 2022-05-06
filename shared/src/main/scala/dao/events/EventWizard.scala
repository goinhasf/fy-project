package dao.events

import io.circe.generic.JsonCodec
import io.circe.generic.extras.ConfiguredJsonCodec
import dao.forms.FormResourceFieldDescriptor
import io.circe.Json
import io.circe.JsonObject
import io.circe.Codec
import io.circe.HCursor
import io.circe.Decoder

@JsonCodec
case class EventWizardDescriptor(
    _id: String,
    societyEventType: SocietyEventType,
    questionIds: Seq[String] = Seq(),
    desc: Option[String] = None
)

@JsonCodec
case class EventWizardState(
    _id: String,
    _descId: String,
    _societyId: String,
    questionStateIds: Map[String, String] = Map()
)

@JsonCodec
case class EventWizardQuestion(
    _id: String,
    title: String,
    options: Map[String, EventWizardQuestionResolver]
)

@JsonCodec
case class QuestionChoice(choice: String, data: Option[JsonObject])

@JsonCodec
case class EventWizardQuestionState(
    _id: String,
    _questionId: String,
    _eventWizardStateId: String,
    selected: Option[QuestionChoice]
)

@ConfiguredJsonCodec
sealed trait EventWizardQuestionResolver {
  val _id: String
  val nextQuestionId: Option[String] = None
}
object EventWizardQuestionResolver {
  import io.circe.generic.extras.Configuration
  implicit val genDevConfig: Configuration = Configuration
    .default
    .withDiscriminator("_t")
}
@JsonCodec
case class NextQuestionResolver(
    override val _id: String,
    override val nextQuestionId: Option[String] = None,
    val extras: Map[String, String] = Map()
) extends EventWizardQuestionResolver

@JsonCodec
case class JsonInputQuestionResolver(
    override val _id: String,
    key: String,
    descriptors: Seq[FormResourceFieldDescriptor] = Seq(),
    override val nextQuestionId: Option[String] = None
) extends EventWizardQuestionResolver {}

/** @param _id The id of the resolver
  * @param formIds A map having as key values the ids of the required form resources
  * @param nextQuestionId The id of the next question
  */
@JsonCodec
case class FormResourcesQuestionResolver(
    override val _id: String,
    formIds: Map[String, Boolean] = Map(),
    override val nextQuestionId: Option[String] = None
) extends EventWizardQuestionResolver
