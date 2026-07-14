import xyz.jpenilla.resourcefactory.paper.PaperPluginYaml.Load

plugins {
    id("plutoproject.paper")
    alias(libs.plugins.resourceFactory.paper)
}

dependencies {
    implementation(projects.platform.common)

    implementation(projects.kernel.paper)
    implementation(projects.foundation.paper)
    implementation(projects.capability.mongo.paper)
    implementation(projects.capability.charonflow.paper)
    implementation(projects.capability.geoip.paper)
    implementation(projects.capability.serverIdentifier.paper)
    implementation(projects.capability.databasePersist.paper)
    implementation(projects.capability.profile.paper)
    implementation(projects.capability.interactive.paper)
    implementation(projects.capability.serverStatistics.paper)
    implementation(projects.capability.worldAlias.paper)
    implementation(projects.capability.legacyCloudCommands.paper)

    implementation(projects.feature.whitelist.paper)
    implementation(projects.feature.gallery.paper)
    implementation(projects.feature.afk.paper)
    implementation(projects.feature.align.paper)
    implementation(projects.feature.creeperFirework.paper)
    implementation(projects.feature.elevator.paper)
    implementation(projects.feature.farmProtection.paper)
    implementation(projects.feature.gm.paper)
    implementation(projects.feature.hat.paper)
    implementation(projects.feature.head.paper)
    implementation(projects.feature.itemframeProtection.paper)
    implementation(projects.feature.lecternProtection.paper)
    implementation(projects.feature.menu.paper)
    implementation(projects.feature.teleport.paper)
    implementation(projects.feature.home.paper)
    implementation(projects.feature.warp.paper)
    implementation(projects.feature.back.paper)
    implementation(projects.feature.randomTeleport.paper)
    implementation(projects.feature.daily.paper)
    implementation(projects.feature.exchangeShop.paper)
    implementation(projects.feature.dynamicScheduler.paper)
    implementation(projects.feature.sit.paper)
    implementation(projects.feature.pvpToggle.paper)
    implementation(projects.feature.recipe.paper)
    implementation(projects.feature.noPlayerCap.paper)
    implementation(projects.feature.noJoinQuitMessage.paper)
    implementation(projects.feature.overloadWarning.paper)
    implementation(projects.feature.suicide.paper)
    implementation(projects.feature.status.paper)
    implementation(projects.feature.devWatermark.paper)
    implementation(projects.feature.recipeUnlock.paper)
    implementation(projects.feature.noCreeperBlockBreaks.paper)
    implementation(libs.bundles.mccoroutine.paper)
    implementation(libs.classgraph)
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
