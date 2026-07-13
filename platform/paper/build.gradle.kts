import xyz.jpenilla.resourcefactory.paper.PaperPluginYaml.Load

plugins {
    id("plutoproject.platform-conventions")
    id("plutoproject.paper-conventions")
    alias(libs.plugins.resourceFactory.paper)
}

dependencies {
    implementation(projects.platform.common)

    implementation(projects.kernel.paper)
    implementation(projects.foundation.paper)
    implementation(projects.capability.mongo.paper)
    implementation(projects.capability.charonflow.paper)
    implementation(projects.capability.serverIdentifier.paper)
    implementation(projects.capability.databasePersist.paper)
    implementation(projects.capability.profile.paper)
    implementation(projects.capability.interactive.paper)
    implementation(projects.capability.serverStatistics.paper)
    implementation(projects.capability.worldAlias.paper)
    implementation(projects.capability.legacyCloudCommands.paper)

    implementation(projects.feature.whitelist.paper)
    implementation(projects.feature.gallery.paper)
}

paperPluginYaml {
    name = "PlutoProject"
    main = "plutoproject.platform.paper.PaperPlatform"
    apiVersion = "26.2"
    author = "Pluto Studio"
    description = "A collection of framework and feature components for the PlutoProject server."
    dependencies {
        server(
            name = "spark",
            load = Load.BEFORE,
            required = false, joinClasspath = true
        )
        server(
            name = "Vault",
            load = Load.BEFORE,
            required = false, joinClasspath = true
        )
        server(
            name = "CoreProtect",
            load = Load.BEFORE,
            required = false, joinClasspath = true
        )
    }
}
