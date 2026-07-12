plugins {
    id("plutoproject.platform-conventions")
    id("plutoproject.velocity-conventions")
    alias(libs.plugins.resourceFactory.velocity)
}

dependencies {
    api(projects.frameworkVelocity)
    api(projects.featureVelocity)
    implementation(project(":kernel:velocity"))
    implementation(project(":capability:mongo:velocity"))
    implementation(project(":capability:server-identifier:velocity"))
    implementation(project(":capability:database-persist:velocity"))
}

velocityPluginJson {
    id = "plutoproject"
    name = "PlutoProject"
    main = "plutoproject.platform.velocity.PlutoVelocityBootstrap"
    authors = listOf("Pluto Studio")
    description = "A collection of framework and feature components for the PlutoProject server."
    dependencies {
        dependency("luckperms", true)
    }
}
