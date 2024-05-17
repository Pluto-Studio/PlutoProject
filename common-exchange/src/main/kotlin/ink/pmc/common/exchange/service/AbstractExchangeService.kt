package ink.pmc.common.exchange.service

import ink.pmc.common.exchange.IExchangeService
import ink.pmc.common.member.api.Member
import java.util.*

abstract class AbstractExchangeService<T> : IExchangeService<T> {

    val id: UUID = UUID.randomUUID()

    override fun noLessThan(member: Member, amount: Long): Boolean {
        return match(member) { it >= amount }
    }

    override fun noMoreThan(member: Member, amount: Long): Boolean {
        return match(member) { it <= amount }
    }

}