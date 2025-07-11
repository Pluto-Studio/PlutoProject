package plutoproject.framework.common.util.data

import org.bson.BsonDocument
import org.bson.BsonValue

fun BsonDocument.getNestedValue(path: String): BsonValue? {
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


fun BsonDocument.setNestedValue(path: String, value: BsonValue) {
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

fun BsonDocument.containsNested(path: String): Boolean {
    val parts = path.split(".")
    var currentValue: BsonValue = this

    for (part in parts) {
        if (currentValue !is BsonDocument) return false
        currentValue = currentValue[part] ?: return false
    }

    return true
}
