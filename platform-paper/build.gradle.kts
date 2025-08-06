import xyz.jpenilla.resourcefactory.paper.PaperPluginYaml.Load

plugins {
    id("plutoproject.platform-conventions")
    id("plutoproject.paper-conventions")
    alias(libs.plugins.resourceFactory.paper)
}

dependencies {
    api(projects.frameworkPaper)
    api(projects.featurePaper)
}

paperPluginYaml {
    name = "PlutoProject"
    main = "plutoproject.platform.paper.PlutoPaperPlatform"
    loader = "plutoproject.platform.paper.PlutoPaperLoader"
    apiVersion = "1.21.8"
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
