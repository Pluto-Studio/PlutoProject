plugins {
    id("plutoproject.velocity-conventions")
}

dependencies {
    api(project(":capability:mongo:velocity"))
    api(projects.featureVelocityApi)
    api(projects.featureCommon)
    ksp(projects.frameworkCommon)
    // KSP Processor 需要
    ksp(libs.kotlinx.serialization.json)
}

ksp {
    arg("feature.moduleId", project.path.replace(":", "_"))
}
