package plutoproject.framework.common.feature

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import kotlinx.serialization.json.Json
import plutoproject.framework.common.api.feature.FeatureMetadata
import plutoproject.framework.common.api.feature.Load
import plutoproject.framework.common.api.feature.Platform
import plutoproject.framework.common.api.feature.metadata.DependencyMetadata
import java.io.OutputStreamWriter

private const val ANNOTATION_CLASS_NAME = "plutoproject.framework.common.api.feature.annotation.Feature"
private const val KSP_OPTION_MODULE_ID = "feature.moduleId"

class FeatureSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val options: Map<String, String>,
) : SymbolProcessor {
    private val features = mutableMapOf<Platform, MutableList<FeatureMetadata>>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(ANNOTATION_CLASS_NAME)
        symbols.filterIsInstance<KSClassDeclaration>().forEach { classDeclaration ->
            processFeatureAnnotation(classDeclaration)
        }
        return emptyList()
    }

    private fun processFeatureAnnotation(classDeclaration: KSClassDeclaration) {
        val annotation = classDeclaration.annotations.first {
            it.shortName.asString() == "Feature"
        }

        val id = annotation.arguments.first { it.name?.asString() == "id" }.value as String
        val platform = annotation.arguments.first { it.name?.asString() == "platform" }.value.let { value ->
            when (value) {
                is KSType -> {
                    val name = value.declaration.simpleName.asString()
                    Platform.valueOf(name)
                }

                is KSClassDeclaration -> {
                    val name = value.simpleName.asString()
                    Platform.valueOf(name)
                }

                else -> error("Unexpected platform type: ${value?.javaClass?.name}")
            }
        }

        val dependencies = annotation.arguments.first { it.name?.asString() == "dependencies" }.value as List<*>
        val dependencyList = dependencies.map { it as KSAnnotation }.map { dependency ->
            val depId = dependency.arguments.first { it.name?.asString() == "id" }.value as String
            val load = dependency.arguments.first { it.name?.asString() == "load" }.value.let { value ->
                when (value) {
                    is KSType -> {
                        val name = value.declaration.simpleName.asString()
                        Load.valueOf(name)
                    }

                    is KSClassDeclaration -> {
                        val name = value.simpleName.asString()
                        Load.valueOf(name)
                    }

                    else -> error("Unexpected load type: ${value?.javaClass?.name}")
                }
            }
            val required = dependency.arguments.first { it.name?.asString() == "required" }.value as Boolean
            DependencyMetadata(depId, load, required)
        }
        val featureMetadata = FeatureMetadata(
            id = id,
            entrypoint = classDeclaration.qualifiedName?.asString() ?: error("Unexpected"),
            platform = platform,
            dependencies = dependencyList
        )

        features.getOrPut(platform) { mutableListOf() }.add(featureMetadata)
    }

    override fun finish() {
        if (features.isEmpty()) return

        val moduleId = options[KSP_OPTION_MODULE_ID]?.trim().orEmpty()
        check(moduleId.isNotEmpty()) {
            "Missing KSP option '$KSP_OPTION_MODULE_ID'. Add in build.gradle.kts: ksp { arg(\"$KSP_OPTION_MODULE_ID\", project.path.replace(\":\", \"_\")) }"
        }

        check(features.size == 1) {
            val platforms = features.keys.joinToString(", ") { it.name }
            "A single Gradle module must only contain features for one platform. Found: $platforms (moduleId=$moduleId)"
        }

        val (platform, platformFeatures) = features.entries.single()
        generateJsonFile(platform, moduleId, platformFeatures)
    }

    private fun generateJsonFile(platform: Platform, moduleId: String, features: List<FeatureMetadata>) {
        val platformDir = when (platform) {
            Platform.PAPER -> "paper"
            Platform.VELOCITY -> "velocity"
        }

        val outputPath = "META-INF/plutoproject/features/$platformDir/$moduleId"
        val json = Json.encodeToString(features.sortedBy { it.id })

        val file = try {
            // KSP 2.x
            val method = codeGenerator::class.java.methods.firstOrNull {
                it.name == "createNewFileByPath" &&
                    it.parameterTypes.contentEquals(arrayOf(Dependencies::class.java, String::class.java, String::class.java))
            } ?: error("createNewFileByPath not found")

            @Suppress("UNCHECKED_CAST")
            method.invoke(codeGenerator, Dependencies(true), outputPath, "json") as java.io.OutputStream
        } catch (_: Throwable) {
            // Fallback: some KSP versions allow path separators in fileName
            codeGenerator.createNewFile(
                dependencies = Dependencies(true),
                packageName = "",
                fileName = outputPath,
                extensionName = "json"
            )
        }

        OutputStreamWriter(file, Charsets.UTF_8).use { it.write(json) }
    }
}
