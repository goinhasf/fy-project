import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

import clients.play.auth.AuthenticationClient
import com.google.inject.AbstractModule
import com.google.inject.Inject
import com.google.inject.name.Names
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import dao.events.EventWizardDescriptor
import dao.events.EventWizardQuestion
import dao.events.SocietyEvent
import dao.forms.FormResource
import dao.forms.FormResourceFields
import dao.societies.Society
import data.ServiceCodecRegistries
import data.repositories._
import endpoints4s.play.server.PlayComponents
import org.mongodb.scala._
import play.api.ConfigLoader
import play.api.Configuration
import play.api.http.FileMimeTypes
import play.api.http.HttpErrorHandler
import play.api.mvc.ControllerComponents
import play.api.mvc.DefaultActionBuilder
import play.api.mvc.PlayBodyParsers
import services.PlayComponentsProvider
import shared.config.MicroServiceConfig
import utils.MicroServiceConfigLoader
import utils._
import dao.events.SocietyEventType
import services.impl.SocietyEventsService
import services.impl.FormSubmissionsService
import clients.play.auth.config.AuthenticationClientConfig
import shared.auth.ApiKey
import shared.config.JwtConfig

class Module extends AbstractModule {

  val config             = ConfigFactory.defaultApplication().resolve()
  val mongoConfig        = MongoConfigLoader.load(config.getConfig("mongo"), "")
  val microServiceConfig = MicroServiceConfigLoader.load(config)
  val mongoClient = MongoClient(
    MongoClientSettings
      .builder()
      .applyConnectionString(new ConnectionString(mongoConfig.address))
      .credential(
        MongoCredential.createCredential(
          mongoConfig.username,
          mongoConfig.dbName,
          mongoConfig.password.toCharArray()
        )
      )
      .build()
  )

  val database = mongoClient
    .getDatabase(mongoConfig.dbName)
    .withCodecRegistry(ServiceCodecRegistries())

  override protected def configure(): Unit = {
    bind(classOf[MongoClient]).toInstance(
      mongoClient
    )

    val formResourcesRepo = new FormResourceRepository(
      database.getCollection(
        mongoConfig.collections(FormResourcesCollection)
      )
    )
    bind(classOf[FormResourceRepository]).toInstance(
      formResourcesRepo
    )
    bind(classOf[FormResourceFieldsRepository]).toInstance(
      new FormResourceFieldsRepositoryImpl(
        database.getCollection(
          mongoConfig.collections(FormResourceFieldsCollection)
        )
      )
    )
    bind(classOf[FormSubmissionsRepository]).toInstance(
      new FormSubmissionsRepositoryImpl(
        database.getCollection(
          mongoConfig.collections(FormSubmissionsCollection)
        )
      )
    )
    bind(classOf[SocietiesRepository]).toInstance(
      new SocietiesRepositoryImpl(
        database.getCollection[Society](
          mongoConfig.collections(SocietiesCollection)
        )
      )
    )
    bind(classOf[EventWizardRepository]).toInstance(
      new EventWizardRepositoryImpl(
        database.getCollection(
          mongoConfig.collections(WizardEventsDescriptorsCollection)
        ),
        database.getCollection(
          mongoConfig.collections(WizardQuestionsCollection)
        )
      )
    )
    bind(classOf[EventWizardStateRepository]).toInstance(
      new EventWizardStateRepositoryImpl(
        database.getCollection(
          mongoConfig.collections(WizardDescriptorsStateCollection)
        ),
        database.getCollection(
          mongoConfig.collections(WizardQuestionsStateCollection)
        )
      )
    )
    bind(classOf[SocietyEventsRepository]).toInstance(
      new SocietyEventsRepositoryImpl(
        database.getCollection(
          mongoConfig.collections(SocietyEventsCollection)
        ),
        database.getCollection(
          mongoConfig.collections(SocietyEventTypesCollection)
        )
      )
    )
    val formSubmissionService = new FormSubmissionsService(
      formResourcesRepo,
      database.getCollection(
        mongoConfig.collections(FormSubmissionsCollection)
      ),
      database.getCollection(
        mongoConfig.collections(SocietiesCollection)
      )
    )
    bind(classOf[FormSubmissionsService]).toInstance(
      formSubmissionService
    )

    bind(classOf[SocietyEventsService]).toInstance(
      new SocietyEventsService(
        database.getCollection(
          mongoConfig.collections(SocietyEventsCollection)
        ),
        formSubmissionService,
        database.getCollection(
          mongoConfig.collections(SocietiesCollection)
        )
      )
    )

    bind(classOf[MicroServiceConfig]).toInstance(microServiceConfig)
    bind(classOf[PlayComponents]).to(classOf[PlayComponentsProvider])
    bind(classOf[AuthenticationClientConfig]).toInstance(
      new AuthenticationClientConfig {
        val apiKey: ApiKey = microServiceConfig.apiKey
        val cookieName: String = config
          .getString("play.http.session.cookieName")
        val jwtConfig: JwtConfig = microServiceConfig.jwtConfig
        val serviceUrl: String   = microServiceConfig.serviceUrl
      }
    )
  }

}
