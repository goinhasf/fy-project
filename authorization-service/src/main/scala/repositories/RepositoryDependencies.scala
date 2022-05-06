package repositories

import org.mongodb.scala.MongoClient

trait RepositoryDependencies {
    val usersRepository: UsersRepository
    val tokensRepository: TokensRepository
    val apiKeysRepository: ApiKeysRepository
}
