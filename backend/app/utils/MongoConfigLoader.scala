package utils

import com.typesafe.config.Config
import play.api.ConfigLoader
import shared.config.MongoConfig
import dao.forms.FormResource
import dao.events.EventWizardDescriptor

object MongoConfigLoader
    extends ConfigLoader[MongoConfig[SocietyManagementAppDBCollections]] {

  override def load(
      config: Config,
      path: String
  ): MongoConfig[SocietyManagementAppDBCollections] = {
    val dbName: String = config.getString("dbName")
    val collections: Map[SocietyManagementAppDBCollections, String] = Map(
      FormResourcesCollection -> config.getString(
        "collections.form-resources"
      ),
      FormCategoriesCollection -> config.getString(
        "collections.form-categories"
      ),
      SocietiesCollection -> config.getString(
        "collections.societies"
      ),
      WizardEventsDescriptorsCollection -> config.getString(
        "collections.event-wizards"
      ),
      WizardQuestionsCollection -> config.getString(
        "collections.event-wizard-questions"
      ),
      SocietyEventsCollection -> config.getString(
        "collections.events"
      ),
      FormResourceFieldsCollection -> config.getString(
        "collections.form-resource-fields"
      ),
      FormSubmissionsCollection -> config.getString(
        "collections.form-submissions"
      ),
      WizardDescriptorsStateCollection -> config.getString(
        "collections.event-wizard-states"
      ),
      WizardQuestionsStateCollection -> config.getString(
        "collections.event-wizard-question-states"
      ),
      SocietyEventTypesCollection -> config.getString(
        "collections.event-types"
      ),
    )
    val address: String  = config.getString("address")
    val username: String = config.getString("username")
    val password: String = config.getString("pwd")

    MongoConfig(
      address,
      dbName,
      collections,
      username,
      password
    )
  }

}

sealed trait SocietyManagementAppDBCollections
object FormResourcesCollection  extends SocietyManagementAppDBCollections
object FormCategoriesCollection extends SocietyManagementAppDBCollections
object SocietiesCollection      extends SocietyManagementAppDBCollections
object WizardEventsDescriptorsCollection
    extends SocietyManagementAppDBCollections
object WizardQuestionsCollection      extends SocietyManagementAppDBCollections
object SocietyEventsCollection        extends SocietyManagementAppDBCollections
object SocietyEventTypesCollection    extends SocietyManagementAppDBCollections
object FormResourceFieldsCollection   extends SocietyManagementAppDBCollections
object FormSubmissionsCollection      extends SocietyManagementAppDBCollections
object WizardQuestionsStateCollection extends SocietyManagementAppDBCollections
object WizardDescriptorsStateCollection
    extends SocietyManagementAppDBCollections
