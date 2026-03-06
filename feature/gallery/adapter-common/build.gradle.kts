plugins {
    id("plutoproject.common-conventions")
}

dependencies {
    api(project(":feature:gallery:api"))
    api(project(":feature:gallery:infra-mongo"))

    // MongoConnection + bson serializers
    api(project(":framework-common-api"))
}
