db = new Mongo()
db.getDB("admin").createUser(
  {
    user: "admin",
    pwd: "example",
    roles: [{ role: "userAdminAnyDatabase", db: "admin" }, "readWriteAnyDatabase"]
  }
)

db.getDB("admin").createUser(
  {
    user: "tester",
    pwd: "password",
    roles: [{ role: "userAdminAnyDatabase", db: "admin" }, "readWriteAnyDatabase"]
  }
)