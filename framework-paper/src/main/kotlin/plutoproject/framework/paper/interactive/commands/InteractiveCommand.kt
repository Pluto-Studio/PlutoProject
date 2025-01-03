package plutoproject.framework.paper.interactive.commands

import cafe.adriel.voyager.navigator.Navigator
import org.bukkit.command.CommandSender
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import plutoproject.framework.paper.api.interactive.startInventory
import plutoproject.framework.paper.interactive.examples.ExampleComposable
import plutoproject.framework.paper.interactive.examples.ExampleScreen1
import plutoproject.framework.paper.interactive.examples.ExampleScreen2
import plutoproject.framework.paper.util.command.ensurePlayer

@Suppress("UNUSED")
object InteractiveCommand {
    private const val PERMISSION = "interactive.example"

    @Command("interactive example_1")
    @Permission(PERMISSION)
    fun CommandSender.example1() = ensurePlayer {
        startInventory {
            Navigator(ExampleScreen1())
        }
    }

    @Command("interactive example_2")
    @Permission(PERMISSION)
    fun CommandSender.example2() = ensurePlayer {
        startInventory {
            Navigator(ExampleScreen2())
        }
    }

    @Command("interactive example_3")
    @Permission(PERMISSION)
    fun CommandSender.example3() = ensurePlayer {
        startInventory {
            ExampleComposable()
        }
    }
}
