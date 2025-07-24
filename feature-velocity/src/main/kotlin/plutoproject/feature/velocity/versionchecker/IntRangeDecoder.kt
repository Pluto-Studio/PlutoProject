package plutoproject.feature.velocity.versionchecker

import com.sksamuel.hoplite.*
import com.sksamuel.hoplite.decoder.NullHandlingDecoder
import com.sksamuel.hoplite.fp.invalid
import com.sksamuel.hoplite.fp.valid
import kotlin.reflect.KType

object IntRangeDecoder : NullHandlingDecoder<IntRange> {
    override fun safeDecode(node: Node, type: KType, context: DecoderContext): ConfigResult<IntRange> {
        return when (node) {
            is ArrayNode -> {
                require(node.size == 2) { "There must be 2 values in range array" }
                val min = node[0]
                val max = node[1]
                require(min is LongNode && max is LongNode) { "Value must be number" }
                (min.value.toInt()..max.value.toInt()).valid()
            }

            else -> ConfigFailure.DecodeError(node, type).invalid()
        }
    }

    override fun supports(type: KType): Boolean {
        return type.classifier == IntRange::class
    }
}
