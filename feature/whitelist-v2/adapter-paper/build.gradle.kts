plugins {
    id("plutoproject.adapter-paper-conventions")
}

dependencies {
    api(project(":feature:whitelist-v2:api"))
    api(project(":feature:whitelist-v2:core"))

    api(project(":feature:whitelist-v2:adapter-common"))

    api(project(":feature:whitelist-v2:infra-mongo"))
    api(project(":feature:whitelist-v2:infra-messaging"))

    // MongoConnection + bson serializers
    api(project(":framework-common-api"))

    api(project(":framework-paper-api"))
    api(project(":feature-paper-api"))
}
