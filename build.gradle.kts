import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.extraProperties

plugins {
    id("java")
    id("java-library")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.shadow)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.paperweight.userdev)
    alias(libs.plugins.resource.factory.bukkit)
    alias(libs.plugins.resource.factory.velocity)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
}

val paperDevBundleVer = "1.21.3-R0.1-SNAPSHOT"
extra["paperDevBundleVer"] = paperDevBundleVer

fun kotlin(s: String): String {
    return "org.jetbrains.kotlin.$s"
}

val bukkitApiVersion by extra("1.21")
val root = project

fun Project.ensureParent(): Boolean {
    return this.parent == rootProject
}

fun <T> tryOrNull(block: () -> T): T? {
    return try {
        block()
    } catch (e: Exception) {
        null
    }
}

fun Project.dependOnOtherModule(name: String, impl: Boolean = false) {
    val par = parent?.name
    val module = tryOrNull { project(":$par:$name") } ?: return
    dependencies {
        if (impl) {
            implementation(module)
        } else {
            compileOnly(module)
        }
    }
}

fun Project.dependOnApi() {
    dependOnOtherModule("api")
}

fun Project.dependOnProto() {
    val par = parent?.name
    val proto = tryOrNull { project(":$par:proto") } ?: return

    dependencies {
        protobuf(proto)
    }
}

fun Project.dependOnShared() {
    dependOnOtherModule("shared")
}

fun Project.configureRelocate() {
    tasks.shadowJar {
        relocate("com.electronwill.nightconfig", "ink.pmc.libs.nightconfig")
    }
}

fun Project.initDevEnv() {
    dependencies {
        subprojects {
            when (project.name) {
                "shared" -> afterEvaluate {
                    useSharedEnv()
                    implementationWithEnv(project)
                    project(project.path) {
                        dependOnApi()
                        dependOnProto()
                    }
                }

                "paper" -> {
                    configurePaperPlugin()
                    afterEvaluate {
                        project(project.path) {
                            enablePaperDevEnv()
                            dependOnApi()
                            dependOnProto()
                            dependOnShared()
                        }
                        implementationWithEnv(project)
                    }
                }

                "velocity" -> {
                    configureVelocityPlugin()
                    afterEvaluate {
                        implementationWithEnv(project)
                        project(project.path) {
                            enableVelocityDevEnv()
                            dependOnApi()
                            dependOnProto()
                            dependOnShared()
                        }
                    }
                }

                "api" -> afterEvaluate {
                    implementationWithEnv(project)
                    project(project.path) {
                        enableApiDevEnv()
                    }
                }

                "proto" -> {
                    protobuf(project)
                }
            }
        }
    }

    configureRelocate()
}

fun Project.usePaperweight() {
    apply {
        plugin("io.papermc.paperweight.userdev")
    }
    paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION
}

fun Project.packageName(): String {
    if (!ensureParent() && parent != null) {
        return "${parent!!.packageName()}.$name"
    }
    return "ink.pmc.${name.lowercase().replace("-", "")}"
}

fun Project.configurePaperPlugin() {
    apply(plugin = "xyz.jpenilla.resource-factory-bukkit-convention")
    afterEvaluate {
        bukkitPluginYaml {
            main = "${parent?.group}.PaperPlugin"
            name = "plutoproject_${parent?.name}"
            apiVersion = bukkitApiVersion
            if (!name.get().contains("runtime")) {
                depend.add("plutoproject_runtime")
            } else return@bukkitPluginYaml
            if (!name.get().contains("framework")) {
                depend.add("plutoproject_framework")
            }
        }
    }
}

fun Project.configureVelocityPlugin() {
    apply(plugin = "xyz.jpenilla.resource-factory-velocity-convention")
    afterEvaluate {
        velocityPluginJson {
            main = "${parent?.group}.VelocityPlugin"
            id = "plutoproject_${parent?.name}"
            name = "plutoproject_${parent?.name}"
            if (!name.get().contains("runtime")) {
                dependency("plutoproject_runtime")
            } else return@velocityPluginJson
            if (!name.get().contains("framework")) {
                dependency("plutoproject_framework")
            }
        }
    }
}

fun Project.extraOrNull(key: String): Any? {
    if (!this.extraProperties.has(key)) {
        return null
    }

    return this.extra[key]
}

fun Project.useSharedEnv() {
    dependencies {
        compileOnly(root.libs.velocity.api)
        compileOnly(root.libs.paper.api)
    }
}

val paperDevEnvProp = "paperDevEnv"
val velocityDevEnvProp = "velocityDevEnv"
val apiDevEnvProp = "apiDevEnv"

fun Project.enablePaperDevEnv() {
    extra[paperDevEnvProp] = true
}

fun Project.enableVelocityDevEnv() {
    extra[velocityDevEnvProp] = true
}

fun Project.enableApiDevEnv() {
    extra[apiDevEnvProp] = true
}

fun Project.usePaperEnv() {
    if (extraOrNull(paperDevEnvProp) != true) {
        return
    }
    usePaperweight()
    configurations.create("obf").extendsFrom(
        configurations.apiElements.get(),
        configurations.runtimeElements.get()
    )
    dependencies {
        paperweight.paperDevBundle(paperDevBundleVer)
    }
}

fun DependencyHandlerScope.implementationWithEnv(dep: Project) {
    if (dep.extraOrNull(paperDevEnvProp) == true) {
        implementation(project(path = dep.path, configuration = "obf"))
    }
    implementation(dep)
}

fun Project.useVelocityEnv() {
    if (extraOrNull(velocityDevEnvProp) != true) {
        return
    }
    dependencies {
        compileOnly(root.libs.velocity)
        kapt(root.libs.velocity.api)
    }
}

fun Project.useApiEnv() {
    if (extraOrNull(apiDevEnvProp) != true) {
        return
    }
    dependencies {
        compileOnly(root.libs.velocity.api)
        compileOnly(root.libs.paper.api)
    }
}

fun Project.useProtoEnv() {
    protobuf {
        protoc {
            artifact = root.libs.protoc.asProvider().get().toString()
        }
        plugins {
            create("grpc") {
                artifact = root.libs.protoc.gen.grpc.java.get().toString()
            }
            create("grpckt") {
                artifact = root.libs.protoc.gen.grpc.kotlin.get().toString() + ":jdk8@jar"
            }
        }
        generateProtoTasks {
            all().forEach {
                it.plugins {
                    create("grpc")
                    create("grpckt")
                }
                it.builtins {
                    create("kotlin")
                }
            }
        }
    }
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = rootProject.libs.plugins.kotlin.jvm.get().pluginId)
    apply(plugin = rootProject.libs.plugins.kotlin.serialization.get().pluginId)
    apply(plugin = rootProject.libs.plugins.kotlin.kapt.get().pluginId)
    apply(plugin = rootProject.libs.plugins.shadow.get().pluginId)
    apply(plugin = rootProject.libs.plugins.protobuf.get().pluginId)
    apply(plugin = rootProject.libs.plugins.jetbrains.compose.get().pluginId)
    apply(plugin = rootProject.libs.plugins.compose.compiler.get().pluginId)

    this.group = packageName()
    this.version = "1.3.0"

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }

    kotlin {
        jvmToolchain(21)
    }

    tasks.compileKotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            javaParameters = true
        }
    }

    repositories {
        mavenCentral()
        mavenLocal()
        google()
        maven(uri("https://jitpack.io"))
        maven(uri("https://oss.sonatype.org/content/repositories/snapshots/"))
        maven(uri("https://repo.papermc.io/repository/maven-public/"))
        maven(uri("https://maven.nostal.ink/repository/maven-public"))
        maven(uri("https://maven.playpro.com/"))
        maven(uri("https://repo.opencollab.dev/main/"))
        maven(uri("https://repo.dmulloy2.net/repository/public/"))
        maven(uri("https://mvn.exceptionflug.de/repository/exceptionflug-public/"))
        maven(uri("https://repo.xenondevs.xyz/releases"))
        maven(uri("https://repo.codemc.io/repository/maven-snapshots/"))
        maven(uri("https://repo.william278.net/releases"))
    }

    fun DependencyHandlerScope.dep(
        dep: Provider<*>,
        dependencyConfiguration: Action<ExternalModuleDependency> = Action { }
    ) {
        if (project.name.contains("runtime")) {
            implementation(dep, dependencyConfiguration)
            return
        }
        compileOnly(dep, dependencyConfiguration)
    }

    dependencies {
        dep(rootProject.libs.bundles.kotlin)
        dep(rootProject.libs.bundles.mccoroutine)
        dep(rootProject.libs.bundles.cloud)
        dep(rootProject.libs.bundles.bytebuddy)
        dep(rootProject.libs.bundles.nightconfig)
        dep(rootProject.libs.bundles.protobuf)
        dep(rootProject.libs.bundles.grpc)
        dep(rootProject.libs.bundles.mongodb)
        dep(rootProject.libs.okhttp)
        dep(rootProject.libs.gson)
        dep(rootProject.libs.catppuccin)
        dep(rootProject.libs.caffeine)
        dep(rootProject.libs.adventure.kt)
        dep(rootProject.libs.bundles.koin)
        dep(rootProject.libs.classgraph)
        dep(provider { compose.runtime })
        dep(provider { compose.runtimeSaveable })
        dep(rootProject.libs.bundles.voyager)
        dep(rootProject.libs.anvilGui)
        compileOnly(rootProject.libs.vault.api) {
            isTransitive = false
        }
        dep(rootProject.libs.bundles.hoplite)
        dep(rootProject.libs.commons.math)
        dep(rootProject.libs.geoip2)
        dep(rootProject.libs.aedile)
        kapt(rootProject.libs.cloud.annotations)
    }

    tasks.shadowJar {
        if (!ensureParent()) {
            return@shadowJar
        }

        archiveClassifier = ""
        onlyIf { project != rootProject && !project.name.startsWith("common-library-") }
        destinationDirectory.set(file("$rootDir/build-outputs"))
    }

    tasks.compileJava {
        options.encoding = "UTF-8"
    }

    if (name != "runtime") {
        tasks.jar {
            manifest {
                attributes["paperweight-mappings-namespace"] = "mojang+yarn"
            }
        }
    }

    configurations.all {
        resolutionStrategy {
            force("com.google.inject:guice:4.2.2")
        }
    }

    useProtoEnv()
}

tasks.shadowJar {
    doFirst { clearOutputsDir() }
}

subprojects {
    if (!ensureParent()) {
        return@subprojects
    }
    initDevEnv()
}

/*
* 需要在 rootProject 中设置。
* 否则下方拓展函数无法正常使用。
* */
project.enablePaperDevEnv()
bukkitPluginYaml {
    main = packageName()
}
velocityPluginJson {
    main = packageName()
}

allprojects {
    fun afterEvaluateIfNonRoot(block: () -> Unit) {
        if (project != rootProject) {
            afterEvaluate {
                block()
            }
            return
        }
        block()
    }

    afterEvaluateIfNonRoot {
        usePaperEnv()
        useVelocityEnv()
        useApiEnv()
    }
}

fun clearOutputsDir() {
    val dir = file("$rootDir/build-outputs")
    if (!dir.exists()) {
        dir.mkdirs()
    }
    file(file("$rootDir/build-outputs")).listFiles()!!.forEach {
        it.delete()
    }
}