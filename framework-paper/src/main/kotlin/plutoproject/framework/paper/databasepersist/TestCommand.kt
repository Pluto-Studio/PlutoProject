package plutoproject.framework.paper.databasepersist

import ink.pmc.advkt.component.text
import ink.pmc.advkt.send
import kotlinx.serialization.Serializable
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import plutoproject.framework.common.api.databasepersist.DatabasePersist
import plutoproject.framework.common.api.databasepersist.adapters.*
import plutoproject.framework.common.util.chat.palettes.mochaText
import plutoproject.framework.common.util.data.serializers.bson.UuidBinarySerializer
import plutoproject.framework.paper.util.command.ensurePlayer
import java.util.*

@Serializable
data class TestObj(
    val identity: @Serializable(UuidBinarySerializer::class) UUID = UUID.randomUUID(),
    val age: Int = 114514,
    val name: String = "Fucked",
    val location: String = "asdadasd",
)

object TestCommand {
    private val objAdapter = SerializationTypeAdapter<TestObj>()

    @Command("database-persist-test-set")
    suspend fun databasePersistSet(sender: CommandSender) = ensurePlayer(sender) {
        val container = DatabasePersist.getContainer(uniqueId)

        container.set("test.string", StringTypeAdapter, "TestString")
        container.set("test.boolean", BooleanTypeAdapter, true)
        container.set("test.int", IntTypeAdapter, Int.MAX_VALUE)
        container.set("test.long", LongTypeAdapter, Long.MAX_VALUE)
        container.set("test.uuid", JavaUuidTypeAdapter, UUID.randomUUID())
        container.set("test.string_list", ListTypeAdapter.String, listOf("1", "2", "3"))
        container.set("test.obj", objAdapter, TestObj())
        container.save()

        send {
            text("完成！") with mochaText
        }
    }

    @Command("database-persist-test-get")
    suspend fun databasePersistGet(sender: CommandSender) = ensurePlayer(sender) {
        val container = DatabasePersist.getContainer(uniqueId)

        val string = container.get("test.string", StringTypeAdapter)
        val boolean = container.get("test.boolean", BooleanTypeAdapter)
        val int = container.get("test.int", IntTypeAdapter)
        val long = container.get("test.long", LongTypeAdapter)
        val uuid = container.get("test.uuid", JavaUuidTypeAdapter)
        val stringList = container.get("test.string_list", ListTypeAdapter.String)
        val obj = container.get("test.obj", objAdapter)

        println(" ")
        println("Getting data from $name's container")
        println("string: $string")
        println("boolean: $boolean")
        println("int: $int")
        println("long: $long")
        println("uuid: $uuid")
        println("stringList: $stringList")
        println("obj: $obj")
        println(" ")

        send {
            text("完成！") with mochaText
        }
    }
}
