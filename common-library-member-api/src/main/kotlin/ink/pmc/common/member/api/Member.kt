package ink.pmc.common.member.api

import ink.pmc.common.member.api.comment.CommentRepository
import ink.pmc.common.member.api.data.DataContainer
import ink.pmc.common.member.api.data.MemberModifier
import ink.pmc.common.member.api.punishment.PunishmentLogger
import java.time.Instant
import java.util.*

@Suppress("UNUSED")
interface Member {

    val uid: Long
    val id: UUID
    val name: String
    val whitelistStatus: WhitelistStatus
    val authType: AuthType
    val createdAt: Instant
    val lastJoinedAt: Instant?
    val dataContainer: DataContainer
    val bedrockAccount: BedrockAccount?
    val bio: String?
    val commentRepository: CommentRepository
    val punishmentLogger: PunishmentLogger
    val modifier: MemberModifier

    fun exemptWhitelist()

    fun grantWhitelist()

    suspend fun linkBedrock(xuid: String, gamertag: String): BedrockAccount?

    suspend fun unlinkBedrock()

    suspend fun update()

    suspend fun refresh()

}