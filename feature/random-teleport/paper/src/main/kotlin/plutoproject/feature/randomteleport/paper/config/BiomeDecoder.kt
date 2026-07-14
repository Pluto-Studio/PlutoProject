package plutoproject.feature.randomteleport.paper.config

import com.sksamuel.hoplite.*
import com.sksamuel.hoplite.decoder.NullHandlingDecoder
import com.sksamuel.hoplite.fp.invalid
import com.sksamuel.hoplite.fp.valid
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.NamespacedKey
import org.bukkit.block.Biome
import kotlin.reflect.KType

object BiomeDecoder : NullHandlingDecoder<Biome> {
    private fun getBiome(name: String): Biome =
        RegistryAccess.registryAccess()
            .getRegistry(RegistryKey.BIOME)
            .getOrThrow(NamespacedKey.minecraft(name.lowercase()))

    override fun safeDecode(node: Node, type: KType, context: DecoderContext): ConfigResult<Biome> {
        return when (node) {
            is StringNode -> getBiome(node.value).valid()
            else -> ConfigFailure.DecodeError(node, type).invalid()
        }
    }

    override fun supports(type: KType): Boolean {
        return type.classifier == Biome::class
    }
}
