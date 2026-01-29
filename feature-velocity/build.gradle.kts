plugins {
    id("plutoproject.velocity-conventions")
}

dependencies {
    api(projects.featureVelocityApi)
    api(projects.featureCommon)
    ksp(projects.frameworkCommon)
    // KSP Processor 需要
    ksp(libs.kotlinx.serialization)
}
