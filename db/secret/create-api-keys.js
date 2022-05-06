db = new Mongo()

const usersDb = db.getDB("users")
const apiKeysCollection = usersDb.createCollection("api-keys")

usersDb.getCollection("api-keys").insert(
    [
        {
            serviceId: "society-management-app",
            key: "DF939C365FA6AADEAA69876A35741",
        },
        {
            serviceId: "files",
            key: "DF939C365FA6AADEAA69876A35741"
        }
    ]
)