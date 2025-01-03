package ink.pmc.whitelist

import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.ResultedEvent
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.LoginEvent
import ink.pmc.advkt.component.component
import ink.pmc.advkt.component.newline
import ink.pmc.advkt.component.text
import ink.pmc.framework.concurrent.submitAsync
import ink.pmc.framework.chat.mochaMaroon
import ink.pmc.whitelist.repositories.WhitelistRepository
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Suppress("UNUSED")
object WhitelistListener : KoinComponent {
    private val repo by inject<WhitelistRepository>()

    @Subscribe(order = PostOrder.FIRST)
    fun LoginEvent.e() = runBlocking {
        player.uniqueId.let {
            if (repo.hasById(it)) return@runBlocking
        }
        result = ResultedEvent.ComponentResult.denied(component {
            text("此 ID 尚未获得白名单") with mochaMaroon
            newline()
            text("若已通过审核，请联系当日的审核员添加") with mochaMaroon
        })
        submitAsync {
            val model = repo.findById(player.uniqueId) ?: return@submitAsync
            if (model.rawName == player.username) return@submitAsync
            model.rawName = player.username
            repo.saveOrUpdate(model)
        }
    }
}