plugins {
    id("plutoproject.velocity-conventions")
}

dependencies {
    api(project(":feature:whitelist-v2:api"))
    api(project(":feature:whitelist-v2:application"))

    api(project(":feature:whitelist-v2:adapter-common"))

    api(project(":feature:whitelist-v2:infra-mongo"))
    api(project(":feature:whitelist-v2:infra-messaging"))

    api(project(":framework-common-api"))
    api(project(":framework-velocity-api"))
    api(project(":feature-velocity-api"))

    ksp(projects.frameworkCommon)
    // KSP Processor 需要
    ksp(libs.kotlinx.serialization)
}

ksp {
    arg("feature.moduleId", project.path.replace(":", "_"))
}
