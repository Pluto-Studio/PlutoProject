plugins {
    id("plutoproject.common-conventions")
}

dependencies {
    api(project(":feature:whitelist-v2:api"))
    api(project(":feature:whitelist-v2:infra-mongo"))

    // MongoConnection + bson serializers
    api(project(":framework-common-api"))
}
