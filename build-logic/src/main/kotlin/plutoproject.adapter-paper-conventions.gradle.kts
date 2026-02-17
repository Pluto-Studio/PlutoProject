plugins {
    id("plutoproject.paper-conventions")
    id("com.google.devtools.ksp")
}

dependencies {
    ksp(project(":framework-common"))
    // KSP Processor 需要
    ksp(libs.kotlinx.serialization)
}

ksp {
    arg("feature.moduleId", project.path.replace(":", "_"))
}
