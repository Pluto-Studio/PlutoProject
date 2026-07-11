package plutoproject.buildsupport.moduleprocessor

import plutoproject.kernel.api.ModuleDescriptor

internal object ModuleDescriptorJson {
    fun encode(descriptor: ModuleDescriptor): String = buildString {
        append('{')
        append("\"schemaVersion\":").append(descriptor.schemaVersion)
        append(",\"id\":").appendString(descriptor.id)
        append(",\"type\":").appendString(descriptor.type.name)
        append(",\"platform\":").appendString(descriptor.platform.name)
        append(",\"entrypoint\":").appendString(descriptor.entrypoint)
        append(",\"requiredFeatures\":").appendStrings(descriptor.requiredFeatures)
        append(",\"optionalFeatures\":").appendStrings(descriptor.optionalFeatures)
        append(",\"requiredCapabilities\":").appendStrings(descriptor.requiredCapabilities)
        append('}')
    }

    private fun StringBuilder.appendStrings(values: List<String>) {
        append(values.joinToString(prefix = "[", postfix = "]") { "\"${escape(it)}\"" })
    }

    private fun StringBuilder.appendString(value: String) {
        append('"').append(escape(value)).append('"')
    }

    private fun escape(value: String) = buildString {
        value.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (character.code < 0x20) {
                    append("\\u").append(character.code.toString(16).padStart(4, '0'))
                } else {
                    append(character)
                }
            }
        }
    }
}
