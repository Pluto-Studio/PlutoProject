plugins {
    id("plutoproject.common-conventions")
}

dependencies {
    api(projects.frameworkCommonApi)
    api(projects.frameworkProto)
    api(libs.ksp.api)
}
