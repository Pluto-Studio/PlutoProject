plugins {
    id("plutoproject.paper-conventions")
}

dependencies {
    api(projects.featurePaperApi)
    api(projects.featureCommon)
    api(project(":capability:mongo:paper"))
    ksp(projects.frameworkCommon)
    // KSP Processor 需要
    ksp(libs.kotlinx.serialization.json)
}

ksp {
    arg("feature.moduleId", project.path.replace(":", "_"))
}
