plugins {
    id("plutoproject.paper-conventions")
}

dependencies {
    api(project(":feature:whitelist-v2:api"))
    api(project(":feature:whitelist-v2:application"))

    api(project(":feature:whitelist-v2:adapter-common"))

    api(project(":feature:whitelist-v2:infra-mongo"))
    api(project(":feature:whitelist-v2:infra-messaging"))

    // MongoConnection + bson serializers
    api(project(":framework-common-api"))

    api(project(":framework-paper-api"))
    api(project(":feature-paper-api"))

    ksp(projects.frameworkCommon)
    // KSP Processor 需要
    ksp(libs.kotlinx.serialization)
}

ksp {
    arg("feature.moduleId", project.path.replace(":", "_"))
}
