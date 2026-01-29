plugins {
    id("plutoproject.paper-conventions")
}

dependencies {
    api(projects.featurePaperApi)
    api(projects.featureCommon)
    ksp(projects.frameworkCommon)
    // KSP Processor 需要
    ksp(libs.kotlinx.serialization)
}
