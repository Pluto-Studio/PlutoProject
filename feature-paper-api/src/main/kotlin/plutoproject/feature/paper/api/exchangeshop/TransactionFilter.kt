package plutoproject.feature.paper.api.exchangeshop

import com.mongodb.client.model.Filters
import org.bson.Document
import org.bson.conversions.Bson

/**
 * 代表交易查询中可被使用的条件。
 */
enum class TransactionFilter(val field: String) {
    ID("id"),
    TIMESTAMP("timestamp"),
    MATERIAL("material"),
    AMOUNT("amount"),
    QUANTITY("quantity"),
    TICKET("ticket"),
    COST("cost"),
    BALANCE("balance"),
}

private sealed interface FilterExpr {
    fun toBson(): Bson

    data class Eq<T : Any>(val field: String, val value: T) : FilterExpr {
        override fun toBson(): Bson = Filters.eq(field, value)
    }

    data class Ne<T : Any>(val field: String, val value: T) : FilterExpr {
        override fun toBson(): Bson = Filters.ne(field, value)
    }

    data class Gt<T : Any>(val field: String, val value: T) : FilterExpr {
        override fun toBson(): Bson = Filters.gt(field, value)
    }

    data class Gte<T : Any>(val field: String, val value: T) : FilterExpr {
        override fun toBson(): Bson = Filters.gte(field, value)
    }

    data class Lt<T : Any>(val field: String, val value: T) : FilterExpr {
        override fun toBson(): Bson = Filters.lt(field, value)
    }

    data class Lte<T : Any>(val field: String, val value: T) : FilterExpr {
        override fun toBson(): Bson = Filters.lte(field, value)
    }

    data class In<T : Any>(val field: String, val values: Iterable<T>) : FilterExpr {
        override fun toBson(): Bson = Filters.`in`(field, values)
    }

    data class Nin<T : Any>(val field: String, val values: Iterable<T>) : FilterExpr {
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
    infix fun <T : Any> TransactionFilter.eq(value: T) {
        filters.add(FilterExpr.Eq(field, value))
    }

    /**
     * 不等于某个值。
     */
    infix fun <T : Any> TransactionFilter.ne(value: T) {
        filters.add(FilterExpr.Ne(field, value))
    }

    /**
     * 大于某个值。
     */
    infix fun <T : Any> TransactionFilter.gt(value: T) {
        filters.add(FilterExpr.Gt(field, value))
    }

    /**
     * 大于等于某个值。
     */
    infix fun <T : Any> TransactionFilter.gte(value: T) {
        filters.add(FilterExpr.Gte(field, value))
    }

    /**
     * 小于某个值。
     */
    infix fun <T : Any> TransactionFilter.lt(value: T) {
        filters.add(FilterExpr.Lt(field, value))
    }

    /**
     * 小于等于某个值。
     */
    infix fun <T : Any> TransactionFilter.lte(value: T) {
        filters.add(FilterExpr.Lte(field, value))
    }

    /**
     * 在某个数组中。
     */
    infix fun <T : Any> TransactionFilter.`in`(values: Iterable<T>) {
        filters.add(FilterExpr.In(field, values))
    }

    /**
     * 不在某个数组中。
     */
    infix fun <T : Any> TransactionFilter.nin(values: Iterable<T>) {
        filters.add(FilterExpr.Nin(field, values))
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
