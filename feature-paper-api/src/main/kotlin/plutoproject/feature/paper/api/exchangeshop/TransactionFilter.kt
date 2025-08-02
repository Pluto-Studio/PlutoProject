package plutoproject.feature.paper.api.exchangeshop

import com.mongodb.client.model.Filters
import org.bson.*
import org.bson.conversions.Bson
import org.bson.types.Decimal128
import java.math.BigDecimal
import java.time.Instant
import java.util.*

/**
 * 代表交易查询中可被使用的条件。
 */
sealed class TransactionFilter<T>(val field: String) {
    abstract fun toBsonValue(value: T): BsonValue

    /**
     * 交易的 ID。
     */
    data object Id : TransactionFilter<UUID>("id") {
        override fun toBsonValue(value: UUID): BsonBinary {
            return BsonBinary(value)
        }
    }

    /**
     * 参与交易的玩家 UUID。
     */
    data object PlayerId : TransactionFilter<UUID>("playerId") {
        override fun toBsonValue(value: UUID): BsonValue {
            return BsonBinary(value)
        }
    }

    /**
     * 交易发生的时间。
     */
    data object Time : TransactionFilter<Instant>("time") {
        override fun toBsonValue(value: Instant): BsonValue {
            return BsonDateTime(value.toEpochMilli())
        }
    }

    /**
     * 交易涉及的商品 ID。
     */
    data object ShopItemId : TransactionFilter<String>("shopItemId") {
        override fun toBsonValue(value: String): BsonValue {
            return BsonString(value)
        }
    }

    /**
     * 交易涉及的物品类型。
     */
    data object ItemType : TransactionFilter<org.bukkit.inventory.ItemType>("itemType") {
        override fun toBsonValue(value: org.bukkit.inventory.ItemType): BsonValue {
            return BsonString(value.key.toString())
        }
    }

    /**
     * 交易的购买数。
     *
     * @see ShopTransaction.amount
     */
    data object Amount : TransactionFilter<Int>("amount") {
        override fun toBsonValue(value: Int): BsonValue {
            return BsonInt32(value)
        }
    }

    /**
     * 交易实际获得的物品堆数量。
     *
     * @see ShopTransaction.quantity
     */
    data object Quantity : TransactionFilter<Int>("quantity") {
        override fun toBsonValue(value: Int): BsonValue {
            return BsonInt32(value)
        }
    }

    /**
     * 交易花费的兑换券。
     */
    data object Ticket : TransactionFilter<Int>("ticket") {
        override fun toBsonValue(value: Int): BsonValue {
            return BsonInt32(value)
        }
    }

    /**
     * 交易花费的货币。
     */
    data object Cost : TransactionFilter<BigDecimal>("cost") {
        override fun toBsonValue(value: BigDecimal): BsonValue {
            return BsonDecimal128(Decimal128(value))
        }
    }

    /**
     * 交易的货币结余。
     */
    data object Balance : TransactionFilter<BigDecimal>("balance") {
        override fun toBsonValue(value: BigDecimal): BsonValue {
            return BsonDecimal128(Decimal128(value))
        }
    }
}

private sealed interface FilterExpr {
    fun toBson(): Bson

    data class Eq(val field: String, val value: BsonValue) : FilterExpr {
        override fun toBson(): Bson = Filters.eq(field, value)
    }

    data class Ne(val field: String, val value: BsonValue) : FilterExpr {
        override fun toBson(): Bson = Filters.ne(field, value)
    }

    data class Gt(val field: String, val value: BsonValue) : FilterExpr {
        override fun toBson(): Bson = Filters.gt(field, value)
    }

    data class Gte(val field: String, val value: BsonValue) : FilterExpr {
        override fun toBson(): Bson = Filters.gte(field, value)
    }

    data class Lt(val field: String, val value: BsonValue) : FilterExpr {
        override fun toBson(): Bson = Filters.lt(field, value)
    }

    data class Lte(val field: String, val value: BsonValue) : FilterExpr {
        override fun toBson(): Bson = Filters.lte(field, value)
    }

    data class In(val field: String, val values: Iterable<BsonValue>) : FilterExpr {
        override fun toBson(): Bson = Filters.`in`(field, values)
    }

    data class Nin(val field: String, val values: Iterable<BsonValue>) : FilterExpr {
        override fun toBson(): Bson = Filters.nin(field, values)
    }
}

/**
 * 交易查询条件 DSL。
 */
class TransactionFilterDsl {
    private val filters = mutableListOf<FilterExpr>()

    /**
     * 等于某个值。
     */
    infix fun <T : Any> TransactionFilter<T>.eq(value: T) {
        filters.add(FilterExpr.Eq(field, toBsonValue(value)))
    }

    /**
     * 不等于某个值。
     */
    infix fun <T : Any> TransactionFilter<T>.ne(value: T) {
        filters.add(FilterExpr.Ne(field, toBsonValue(value)))
    }

    /**
     * 大于某个值。
     */
    infix fun <T : Any> TransactionFilter<T>.gt(value: T) {
        filters.add(FilterExpr.Gt(field, toBsonValue(value)))
    }

    /**
     * 大于等于某个值。
     */
    infix fun <T : Any> TransactionFilter<T>.gte(value: T) {
        filters.add(FilterExpr.Gte(field, toBsonValue(value)))
    }

    /**
     * 小于某个值。
     */
    infix fun <T : Any> TransactionFilter<T>.lt(value: T) {
        filters.add(FilterExpr.Lt(field, toBsonValue(value)))
    }

    /**
     * 小于等于某个值。
     */
    infix fun <T : Any> TransactionFilter<T>.lte(value: T) {
        filters.add(FilterExpr.Lte(field, toBsonValue(value)))
    }

    /**
     * 在某个数组中。
     */
    infix fun <T : Any> TransactionFilter<T>.`in`(values: Iterable<T>) {
        filters.add(FilterExpr.In(field, values.map { toBsonValue(it) }))
    }

    /**
     * 不在某个数组中。
     */
    infix fun <T : Any> TransactionFilter<T>.nin(values: Iterable<T>) {
        filters.add(FilterExpr.Nin(field, values.map { toBsonValue(it) }))
    }

    /**
     * 构造 Bson 查询条件。
     */
    fun build(): Bson {
        return when {
            filters.isEmpty() -> Document()
            filters.size == 1 -> filters.first().toBson()
            else -> Filters.and(filters.map { it.toBson() })
        }
    }
}
