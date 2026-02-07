plugins {
    id("plutoproject.velocity-conventions")
}

dependencies {
    api(projects.featureVelocityApi)
    api(projects.featureCommon)
    api(project(":feature:whitelist-v2:velocity"))
    ksp(projects.frameworkCommon)
    // KSP Processor 需要
    ksp(libs.kotlinx.serialization)
}
