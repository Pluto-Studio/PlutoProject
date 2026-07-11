package plutoproject.buildsupport.moduleprocessor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.getConstructors
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.validate
import java.io.OutputStreamWriter
import plutoproject.kernel.api.MODULE_DESCRIPTOR_SCHEMA_VERSION
import plutoproject.kernel.api.ModuleDescriptor
import plutoproject.kernel.api.ModuleType
import plutoproject.kernel.api.Platform

private const val FEATURE_ANNOTATION = "plutoproject.kernel.api.Feature"
private const val CAPABILITY_ANNOTATION = "plutoproject.kernel.api.Capability"
private const val RUNTIME_MODULE = "plutoproject.kernel.api.RuntimeModule"
private const val PROJECT_PATH_OPTION = "runtimeModule.projectPath"

internal class RuntimeModuleSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>,
) : SymbolProcessor {
    private val declarations = linkedMapOf<String, Pair<ModuleDeclaration, KSFile?>>()
    private var hasErrors = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = sequenceOf(FEATURE_ANNOTATION, CAPABILITY_ANNOTATION)
            .flatMap(resolver::getSymbolsWithAnnotation)
            .toList()
        val deferred = symbols.filterNot(KSAnnotated::validate)
        symbols.filter(KSAnnotated::validate).forEach(::processSymbol)
        return deferred
    }

    private fun processSymbol(symbol: KSAnnotated) {
        val declaration = symbol as? KSClassDeclaration
        if (declaration == null) {
            logger.error("Runtime module annotations can only target classes", symbol)
            hasErrors = true
            return
        }
        val qualifiedName = declaration.qualifiedName?.asString()
        if (qualifiedName == null || qualifiedName in declarations) return

        val feature = declaration.findAnnotation(FEATURE_ANNOTATION)
        val capability = declaration.findAnnotation(CAPABILITY_ANNOTATION)
        if (feature != null && capability != null) {
            logger.error("An entrypoint cannot be both a feature and a capability", declaration)
            hasErrors = true
            return
        }
        val annotation = feature ?: capability ?: return
        val type = if (feature != null) ModuleType.FEATURE else ModuleType.CAPABILITY
        val descriptor = ModuleDescriptor(
            schemaVersion = MODULE_DESCRIPTOR_SCHEMA_VERSION,
            id = annotation.stringArgument("id"),
            type = type,
            platform = annotation.enumArgument("platform"),
            entrypoint = qualifiedName,
            requiredFeatures = annotation.stringListArgument("requiredFeatures"),
            optionalFeatures = annotation.stringListArgument("optionalFeatures"),
            requiredCapabilities = annotation.stringListArgument("requiredCapabilities"),
        )
        val moduleDeclaration = ModuleDeclaration(
            descriptor = descriptor,
            isClass = declaration.classKind == ClassKind.CLASS,
            isPublic = declaration.modifiers.none {
                it == Modifier.PRIVATE || it == Modifier.PROTECTED || it == Modifier.INTERNAL
            },
            isAbstract = Modifier.ABSTRACT in declaration.modifiers,
            isInner = Modifier.INNER in declaration.modifiers,
            implementsRuntimeModule = declaration.implementsRuntimeModule(),
            hasPublicZeroArgumentConstructor = declaration.hasPublicZeroArgumentConstructor(),
        )
        val errors = ModuleDeclarationValidator.validate(moduleDeclaration)
        errors.forEach { logger.error(it, declaration) }
        hasErrors = hasErrors || errors.isNotEmpty()
        declarations[qualifiedName] = moduleDeclaration to declaration.containingFile
    }

    override fun finish() {
        if (hasErrors || declarations.isEmpty()) return
        val projectPath = options[PROJECT_PATH_OPTION]?.trim().orEmpty()
        if (projectPath.isEmpty()) {
            logger.error("Missing KSP option '$PROJECT_PATH_OPTION'; apply plutoproject.runtime-module")
            return
        }
        val compilationErrors = ModuleCompilationValidator.validate(declarations.values.map { it.first })
        if (compilationErrors.isNotEmpty()) {
            compilationErrors.forEach { logger.error("$it ($projectPath)") }
            return
        }
        declarations.values.sortedBy { it.first.descriptor.id }.forEach { (declaration, source) ->
            writeDescriptor(declaration.descriptor, source)
        }
    }

    private fun writeDescriptor(descriptor: ModuleDescriptor, source: KSFile?) {
        val path = "META-INF/plutoproject/modules/${descriptor.platform.resourceDirectory}/${descriptor.id}.json"
        val dependencies = source?.let { Dependencies(false, it) } ?: Dependencies.ALL_FILES
        codeGenerator.createNewFileByPath(dependencies, path, "").use { output ->
            OutputStreamWriter(output, Charsets.UTF_8).use {
                it.write(ModuleDescriptorJson.encode(descriptor))
            }
        }
    }

    private fun KSClassDeclaration.implementsRuntimeModule(visited: MutableSet<String> = mutableSetOf()): Boolean {
        val name = qualifiedName?.asString() ?: return false
        if (!visited.add(name)) return false
        if (name == RUNTIME_MODULE) return true
        return superTypes.any { reference ->
            (reference.resolve().declaration as? KSClassDeclaration)?.implementsRuntimeModule(visited) == true
        }
    }

    private fun KSClassDeclaration.hasPublicZeroArgumentConstructor(): Boolean =
        getConstructors().any { constructor ->
            constructor.modifiers.none {
                it == Modifier.PRIVATE || it == Modifier.PROTECTED || it == Modifier.INTERNAL
            } && constructor.parameters.all { it.hasDefault }
        }

    private fun KSClassDeclaration.findAnnotation(name: String): KSAnnotation? = annotations.firstOrNull {
        it.annotationType.resolve().declaration.qualifiedName?.asString() == name
    }

    private fun KSAnnotation.argument(name: String): Any? =
        arguments.firstOrNull { it.name?.asString() == name }?.value

    private fun KSAnnotation.stringArgument(name: String): String = argument(name) as? String ?: ""

    private fun KSAnnotation.stringListArgument(name: String): List<String> =
        (argument(name) as? List<*>)?.filterIsInstance<String>().orEmpty()

    private fun KSAnnotation.enumArgument(name: String): Platform {
        val value = argument(name)
        val enumName = when (value) {
            is KSType -> value.declaration.simpleName.asString()
            is KSClassDeclaration -> value.simpleName.asString()
            else -> error("Unexpected enum value for $name: $value")
        }
        return Platform.valueOf(enumName)
    }
}
