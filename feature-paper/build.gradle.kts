plugins {
    id("plutoproject.paper-conventions")
}

dependencies {
    api(projects.featurePaperApi)
    api(projects.featureCommon)
    api(project(":feature:whitelist-v2:adapter-paper"))
    ksp(projects.frameworkCommon)
    // KSP Processor 需要
    ksp(libs.kotlinx.serialization)
}

ksp {
    arg("feature.moduleId", project.path.replace(":", "_"))
}
