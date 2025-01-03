plugins {
    id("plutoproject.platform-conventions")
    id("plutoproject.velocity-conventions")
    alias(libs.plugins.resourceFactory.velocity)
}

dependencies {
    api(projects.frameworkVelocity)
    api(projects.featureVelocity)
}

velocityPluginJson {
    id = "plutoproject"
    name = "PlutoProject"
    main = "plutoproject.platform.velocity.PlutoVelocityBootstrap"
    authors = listOf("nostalfinals", "PlutoProject Contributors")
}