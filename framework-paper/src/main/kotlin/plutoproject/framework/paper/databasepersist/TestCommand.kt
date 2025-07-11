package plutoproject.framework.paper.databasepersist

import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import plutoproject.framework.common.api.databasepersist.adapters.SerializationTypeAdapter
import kotlin.uuid.ExperimentalUuidApi

object TestCommand {
    @OptIn(ExperimentalUuidApi::class)
    @Command("database_persist_test")
    suspend fun databasePersistTest(sender: CommandSender) {
        val adapter = SerializationTypeAdapter<TestModel>()
        val bson = adapter.toBson(TestModel())
        println("Encode - class: ${bson::class.qualifiedName}, value: $bson")
        val decode = adapter.fromBson(bson)
        println("Decode - class: ${decode::class.qualifiedName}, value: $decode")
    }
}
