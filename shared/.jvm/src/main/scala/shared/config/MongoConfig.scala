package shared.config

case class MongoConfig[CollectionType](
    address: String,
    dbName: String,
    collections: Map[CollectionType, String],
    username: String,
    password: String
)
