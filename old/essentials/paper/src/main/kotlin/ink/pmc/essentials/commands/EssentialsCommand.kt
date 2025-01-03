package ink.pmc.essentials.commands

import ink.pmc.essentials.COMMAND_ESS_RTP
import ink.pmc.essentials.COMMAND_ESS_RTP_PERF_END
import ink.pmc.essentials.COMMAND_ESS_RTP_PERF_START
import ink.pmc.essentials.teleport.random.PerfTest
import ink.pmc.framework.command.ensurePlayer
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission

@Suppress("UNUSED")
object EssentialsCommand {
    @Command("essentials|ess rtp")
    @Permission("essentials.cmd")
    fun CommandSender.rtp() = ensurePlayer {
        sendMessage(COMMAND_ESS_RTP)
    }

    @Command("essentials|ess rtp_perf_test")
    @Permission("essentials.cmd")
    fun CommandSender.rtpPerfTest() = ensurePlayer {
        if (!PerfTest.isInTest(this)) {
            PerfTest.startTest(this)
            sendMessage(COMMAND_ESS_RTP_PERF_START)
        } else {
            PerfTest.endTest(this)
            sendMessage(COMMAND_ESS_RTP_PERF_END)
        }
    }
}