plugins {
    id("plutoproject.common-conventions")
}

dependencies {
    api(project(":feature:gallery:api"))
    api(project(":feature:gallery:infra-mongo"))
    api(project(":feature:gallery:frontend"))

    // MongoConnection + bson serializers
    api(project(":framework-common-api"))
}
