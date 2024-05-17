package ink.pmc.common.exchange

import ink.pmc.common.member.api.Member

abstract class AbstractExchangeService<T> : IExchangeService<T> {

    override fun noLessThan(member: Member, amount: Long): Boolean {
        return match(member) { it >= amount }
    }

    override fun noMoreThan(member: Member, amount: Long): Boolean {
        return match(member) { it <= amount }
    }

}