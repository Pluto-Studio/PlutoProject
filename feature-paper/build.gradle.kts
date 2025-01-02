plugins {
    id("plutoproject.paper-conventions")
}

dependencies {
    api(projects.featurePaperApi)
    api(projects.featureCommon)
    ksp(projects.frameworkCommon)
}
