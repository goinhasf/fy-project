db = new Mongo()

db.getDB("society-management-app").createUser(
    {
        user: "soc-app",
        pwd: "secret",
        roles: [
            {
                role: "readWrite", db: "society-management-app"
            }
        ]
    }
)

db.getDB("files").createUser(
    {
        user: "files-service",
        pwd: "secret",
        roles: [
            {
                role: "readWrite", db: "files"
            }
        ]
    }
)

db.getDB("users").createUser(
    {
        user: "authorization-service",
        pwd: "secret",
        roles: [
            {
                role: "readWrite", db: "users"
            }
        ]
    }
)


