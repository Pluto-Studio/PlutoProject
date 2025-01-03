package plutoproject.framework.common.util.config.decoders

import com.sksamuel.hoplite.*
import com.sksamuel.hoplite.decoder.NullHandlingDecoder
import com.sksamuel.hoplite.fp.invalid
import com.sksamuel.hoplite.fp.valid
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import kotlin.reflect.KType

object ComponentDecoder : NullHandlingDecoder<Component> {
    override fun safeDecode(node: Node, type: KType, context: DecoderContext): ConfigResult<Component> {
        return when (node) {
            is StringNode -> MiniMessage.miniMessage().deserialize(node.value).valid()
            else -> ConfigFailure.DecodeError(node, type).invalid()
        }
    }

    override fun supports(type: KType): Boolean {
        return type.classifier == Component::class
    }
}
