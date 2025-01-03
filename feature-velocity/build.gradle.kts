plugins {
    id("plutoproject.velocity-conventions")
}

dependencies {
    api(projects.featureVelocityApi)
    api(projects.featureCommon)
    ksp(projects.frameworkCommon)
}
