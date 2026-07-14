plugins {
    id("plutoproject.velocity")
    alias(libs.plugins.resourceFactory.velocity)
}

dependencies {
    implementation(projects.platform.common)

    implementation(projects.kernel.velocity)
    implementation(projects.foundation.velocity)

    implementation(projects.capability.mongo.velocity)
    implementation(projects.capability.charonflow.velocity)
    implementation(projects.capability.geoip.velocity)
    implementation(projects.capability.serverIdentifier.velocity)
    implementation(projects.capability.databasePersist.velocity)
    implementation(projects.capability.profile.velocity)
    implementation(projects.capability.legacyCloudCommands.velocity)

    implementation(projects.feature.whitelist.velocity)
    implementation(projects.feature.joinQuitMessage.velocity)
    implementation(projects.feature.motd.velocity)
    implementation(projects.feature.playerCap.velocity)
    implementation(projects.feature.versionChecker.velocity)
    implementation(libs.bundles.mccoroutine.velocity)
}

velocityPluginJson {
    id = "plutoproject"
    name = "PlutoProject"
    main = "plutoproject.platform.velocity.VelocityPlatform"
    authors = listOf("Pluto Studio")
    description = "A collection of framework and feature components for the PlutoProject server."
    dependencies {
        dependency("luckperms", true)
    }
}
