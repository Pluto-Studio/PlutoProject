package plutoproject.framework.common.util.data

import org.bson.BsonDocument
import org.bson.BsonValue

fun BsonDocument.getNested(path: String): BsonValue? {
    val parts = path.split(".")
    var currentValue: BsonValue = this

    for ((index, part) in parts.withIndex()) {
        if (currentValue !is BsonDocument) return null
        val nextValue = currentValue[part] ?: return null
        if (index == parts.lastIndex) {
            return nextValue
        } else {
            if (nextValue !is BsonDocument) return null
            currentValue = nextValue
        }
    }

    return null
}

fun BsonDocument.setNested(path: String, value: BsonValue) {
    val parts = path.split(".")
    var currentDoc = this.toBsonDocument()

    for ((index, part) in parts.withIndex()) {
        if (index == parts.lastIndex) {
            currentDoc[part] = value
        } else {
            val next = currentDoc[part]
            if (next !is BsonDocument) {
                val newDoc = BsonDocument()
                currentDoc[part] = newDoc
                currentDoc = newDoc
            } else {
                currentDoc = next
            }
        }
    }
}

fun BsonDocument.flatten(prefix: String = ""): BsonDocument {
    val result = BsonDocument()
    for ((key, value) in this) {
        val fullKey = if (prefix.isEmpty()) key else "$prefix.$key"
        when (value) {
            is BsonDocument -> result += value.flatten(fullKey)
            else -> result[fullKey] = value
        }
    }
    return result
}

fun Map<String, BsonValue>.toBsonDocument(): BsonDocument {
    return BsonDocument().also { it.putAll(this) }
}
