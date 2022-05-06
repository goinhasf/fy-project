package repositories

import scala.concurrent.Future
import shared.auth.ApiKey
import org.mongodb.scala.model.Filters._
import org.mongodb.scala._
import io.circe.parser._

trait ApiKeysRepository {
  def findApiKey(key: ApiKey): Future[Option[ApiKey]]
}

class ApiKeysRepositoryImpl(apiKeysCollection: MongoCollection[Document])
    extends ApiKeysRepository {
  def findApiKey(key: ApiKey): Future[Option[ApiKey]] = apiKeysCollection
    .find(
      and(
        equal("serviceId", key.serviceId),
        equal("key", key.key)
      )
    )
    .map(_.toJson())
    .map(decode[ApiKey](_).fold(throw _, identity))
    .headOption()
}
