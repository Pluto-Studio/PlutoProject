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

class FeatureSymbolProcessor(
    private val codeGenerator: CodeGenerator
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
        features.forEach { (platform, platformFeatures) ->
            generateJsonFile(platform, platformFeatures)
        }
    }

    private fun generateJsonFile(platform: Platform, features: List<FeatureMetadata>) {
        val json = Json.encodeToString(features)
        val file = codeGenerator.createNewFile(
            dependencies = Dependencies(true),
            packageName = "",
            fileName = platform.platformManifestFileName,
            extensionName = "json"
        )
        OutputStreamWriter(file, "UTF-8").use { it.write(json) }
    }
}
