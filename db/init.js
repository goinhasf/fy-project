function createCollections(db) {

    const filesDb = db.getDB("files")
    filesDb.createCollection("metadata")
    filesDb.getCollection("metadata").createIndex({ fileId: 1 })

    filesDb.getCollection("metadata").insert(
        [
            {
                "fileId": "03a18945-b1c5-4ad7-9d0f-f43e93f5fbfb",
                "fileName": "External Payments.docx",
                "size": 323237,
                "contentType": "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            },
            {
                "fileId": "b16d0d2e-b4c4-4037-881b-5ed4df66b804",
                "fileName": "Ticket Request Form.docx",
                "size": 31559,
                "contentType": "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            },
            {
                "fileId": "dc9f616b-2c25-441f-9d17-bece7c1b9e6e",
                "fileName": "Prepared Risk Assessment Socials.docx",
                "size": 26560,
                "contentType": "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            }
        ]
    )

    const socManagementDb = db.getDB("society-management-app")
    socManagementDb.createCollection("form-resources")
    socManagementDb.createCollection("form-resource-fields")
    socManagementDb.createCollection("form-submissions")
    socManagementDb.createCollection("form-categories")

    socManagementDb.createCollection("societies")

    socManagementDb.createCollection("event-wizard-questions")
    socManagementDb.createCollection("event-wizard-question-states")
    socManagementDb.createCollection("event-wizards")
    socManagementDb.createCollection("event-wizard-states")
    socManagementDb.createCollection("events")
    socManagementDb.createCollection("event-types")
    socManagementDb.getCollection("event-types").insertOne({
        "name": "Social"
    })


    const usersDb = db.getDB("users")
    usersDb.createCollection("user")
    const userId = new ObjectId
    usersDb.getCollection("user").insertOne(
        {
            "_id": userId.valueOf(),
            "userInfo": {
                "id": userId.valueOf(),
                "firstName": "Test",
                "lastName": "User",
                "email": "test@tester.com",
                "role": {
                    "roleType": { "_t": "AdminRole" },
                    "privileges": [
                        { "name": "system-resources-management", "scopes": {} },
                        { "name": "system-users-management", "scopes": {} }
                    ]
                }
            },
            "credentials": {
                "email": {
                    "email": "test@tester.com"
                },
                "password": {
                    "salt": {
                        "value": "34e67a8bc9e4f674551024183037dfa63c37a6db2f47d318e3c028fcad0f32f5"
                    },
                    "hash": {
                        "value": "0b2541959909aa4931fd3ad1638eb36fd980cea9f7f1115f143f507ebf7d824c",
                        "alg": "SHA-256"
                    }
                }
            }

        }
    )
    const adminId = new ObjectId
    usersDb.getCollection("user").insertOne(
        {
            "_id": adminId.valueOf(),
            "userInfo": {
                "id": adminId.valueOf(),
                "firstName": "Guild",
                "lastName": "User",
                "email": "guild-user@admin.com",
                "role": {
                    "roleType": { "_t": "AdminRole" },
                    "privileges": []
                }
            },
            "credentials": {
                "email": {
                    "email": "guild-user@admin.com"
                },
                "password": {
                    "salt": {
                        "value": "34e67a8bc9e4f674551024183037dfa63c37a6db2f47d318e3c028fcad0f32f5"
                    },
                    "hash": {
                        "value": "0b2541959909aa4931fd3ad1638eb36fd980cea9f7f1115f143f507ebf7d824c",
                        "alg": "SHA-256"
                    }
                }
            }

        }
    )
    usersDb.createCollection("token")
    const regularUserId = new ObjectId
    usersDb.getCollection("user").insertOne(
        {
            "_id": regularUserId.valueOf(),
            "userInfo": {
                "id": regularUserId.valueOf(),
                "firstName": "Regular",
                "lastName": "User",
                "email": "regular-user@user.com",
                "role": {
                    "roleType": { "_t": "RegularUserRole" },
                    "privileges": []
                }
            },
            "credentials": {
                "email": {
                    "email": "regular-user@user.com"
                },
                "password": {
                    "salt": {
                        "value": "34e67a8bc9e4f674551024183037dfa63c37a6db2f47d318e3c028fcad0f32f5"
                    },
                    "hash": {
                        "value": "0b2541959909aa4931fd3ad1638eb36fd980cea9f7f1115f143f507ebf7d824c",
                        "alg": "SHA-256"
                    }
                }
            }

        }
    )
}

db = new Mongo()
createCollections(db)
load("/docker-entrypoint-initdb.d/secret/create-api-keys.js")
load("/docker-entrypoint-initdb.d/secret/create-db-users.js")
load("/docker-entrypoint-initdb.d/secret/secure-db.js")