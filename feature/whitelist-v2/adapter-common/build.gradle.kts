plugins {
    id("plutoproject.common-conventions")
}

dependencies {
    api(project(":feature:whitelist-v2:core"))

    api(project(":feature:whitelist-v2:infra-mongo"))
    api(project(":feature:whitelist-v2:infra-messaging"))

    // MongoConnection + bson serializers
    api(project(":framework-common-api"))
}
