package plutoproject.feature.whip.paper

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import plutoproject.foundation.common.text.replace

const val WHIP_NAMESPACE = "plutoproject_whip"

private const val WHIP_TYPE = "whip"
private const val WHIP_DATA_VERSION = 1

private val WHIP_ITEM_TYPE_KEY = NamespacedKey(WHIP_NAMESPACE, "item_type")
private val WHIP_DATA_VERSION_KEY = NamespacedKey(WHIP_NAMESPACE, "data_version")
private val WHIP_LEVEL_KEY = NamespacedKey(WHIP_NAMESPACE, "level")
private val WHIP_OWNED_KEYS = setOf(
    WHIP_ITEM_TYPE_KEY,
    WHIP_DATA_VERSION_KEY,
    WHIP_LEVEL_KEY,
)

enum class WhipLevel(
    val number: Int,
    val roman: String,
) {
    I(1, "I"),
    II(2, "II"),
    III(3, "III"),
    IV(4, "IV"),
    V(5, "V");

    companion object {
        fun parse(value: String): WhipLevel? {
            val normalized = value.trim().uppercase()
            return entries.firstOrNull { it.roman == normalized || it.number.toString() == normalized }
        }

        fun fromNumber(number: Int): WhipLevel? = entries.firstOrNull { it.number == number }
    }
}

sealed interface WhipIdentity {
    data object NotWhip : WhipIdentity
    data object Invalid : WhipIdentity
    data class Valid(val level: WhipLevel) : WhipIdentity
}

@Suppress("UnstableApiUsage")
fun createWhipItem(level: WhipLevel, config: WhipConfig): ItemStack {
    val item = ItemStack(Material.LEAD)
    item.setData(DataComponentTypes.MAX_STACK_SIZE, 1)
    item.setData(DataComponentTypes.ITEM_NAME, WHIP_ITEM_NAME)
    item.setData(
        DataComponentTypes.LORE,
        ItemLore.lore(
            listOf(
                WHIP_ITEM_LORE_LEVEL.replace(WHIP_PLACEHOLDER_LEVEL, level.roman),
                WHIP_ITEM_LORE_LENGTH.replace(WHIP_PLACEHOLDER_LENGTH, config.length(level)),
            ),
        ),
    )
    item.itemMeta = item.itemMeta.apply {
        persistentDataContainer.set(WHIP_ITEM_TYPE_KEY, PersistentDataType.STRING, WHIP_TYPE)
        persistentDataContainer.set(WHIP_DATA_VERSION_KEY, PersistentDataType.INTEGER, WHIP_DATA_VERSION)
        persistentDataContainer.set(WHIP_LEVEL_KEY, PersistentDataType.INTEGER, level.number)
    }
    return item
}

fun inspectWhip(item: ItemStack?): WhipIdentity {
    if (item == null || item.type.isAir || item.amount <= 0) {
        return WhipIdentity.NotWhip
    }

    val container = item.itemMeta.persistentDataContainer
    if (container.keys.none { it in WHIP_OWNED_KEYS }) {
        return WhipIdentity.NotWhip
    }
    if (item.type != Material.LEAD) {
        return WhipIdentity.Invalid
    }

    val type = container.get(WHIP_ITEM_TYPE_KEY, PersistentDataType.STRING)
    val version = container.get(WHIP_DATA_VERSION_KEY, PersistentDataType.INTEGER)
    val level = container.get(WHIP_LEVEL_KEY, PersistentDataType.INTEGER)
    val parsedLevel = level?.let(WhipLevel::fromNumber)
    return if (type == WHIP_TYPE && version == WHIP_DATA_VERSION && parsedLevel != null) {
        WhipIdentity.Valid(parsedLevel)
    } else {
        WhipIdentity.Invalid
    }
}

fun ItemStack.whipLevelOrNull(): WhipLevel? =
    (inspectWhip(this) as? WhipIdentity.Valid)?.level

fun ItemStack.isOrdinaryLead(): Boolean =
    type == Material.LEAD && inspectWhip(this) == WhipIdentity.NotWhip
