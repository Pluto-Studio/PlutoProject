package plutoproject.framework.common.util.config.decoders

import com.sksamuel.hoplite.*
import com.sksamuel.hoplite.decoder.NullHandlingDecoder
import com.sksamuel.hoplite.fp.invalid
import com.sksamuel.hoplite.fp.valid
import kotlin.reflect.KType

object CharDecoder : NullHandlingDecoder<Char> {
    override fun safeDecode(node: Node, type: KType, context: DecoderContext): ConfigResult<Char> {
        return when (node) {
            is StringNode -> {
                val value = node.value
                check(value.length == 1) { "There is and must be one character" }
                value.first().valid()
            }

            else -> ConfigFailure.DecodeError(node, type).invalid()
        }
    }

    override fun supports(type: KType): Boolean {
        return type.classifier == Char::class
    }
}
