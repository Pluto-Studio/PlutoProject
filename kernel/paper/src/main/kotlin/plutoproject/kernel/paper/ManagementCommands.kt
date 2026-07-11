package plutoproject.kernel.paper

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import kotlinx.coroutines.runBlocking
import org.bukkit.command.CommandSender
import plutoproject.kernel.api.ModuleType
import plutoproject.kernel.common.ModuleManagementService

internal fun createManagementCommand(
    management: ModuleManagementService,
): LiteralArgumentBuilder<CommandSourceStack> = Commands.literal("plutoproject")
    .then(
        Commands.literal("feature")
            .then(
                Commands.literal("list")
                    .requiresPermission(FEATURE_LIST_PERMISSION)
                    .executes { context ->
                        val modules = management.snapshots().filter { it.descriptor.type == ModuleType.FEATURE }
                        context.source.sender.sendMessage(getModuleListMessage("Feature", modules))
                        Command.SINGLE_SUCCESS
                    },
            )
            .then(infoCommand(FEATURE_INFO_PERMISSION, ModuleType.FEATURE, management))
            .then(
                Commands.literal("disable")
                    .requiresPermission(FEATURE_DISABLE_PERMISSION)
                    .then(
                        Commands.argument("id", StringArgumentType.word())
                            .suggestsModuleIds(ModuleType.FEATURE, management)
                            .executes { context ->
                                val result = runBlocking {
                                    management.disable(StringArgumentType.getString(context, "id"))
                                }
                                context.source.sender.sendMessage(getDisableResultMessage(result))
                                Command.SINGLE_SUCCESS
                            },
                    ),
            ),
    )
    .then(
        Commands.literal("capability")
            .then(
                Commands.literal("list")
                    .requiresPermission(CAPABILITY_LIST_PERMISSION)
                    .executes { context ->
                        val modules = management.snapshots().filter { it.descriptor.type == ModuleType.CAPABILITY }
                        context.source.sender.sendMessage(getModuleListMessage("Capability", modules))
                        Command.SINGLE_SUCCESS
                    },
            )
            .then(infoCommand(CAPABILITY_INFO_PERMISSION, ModuleType.CAPABILITY, management)),
    )
    .then(
        Commands.literal("module")
            .then(
                Commands.literal("graph")
                    .requiresPermission(MODULE_GRAPH_PERMISSION)
                    .then(
                        Commands.argument("id", StringArgumentType.word())
                            .suggestsModuleIds(null, management)
                            .executes { context ->
                                val id = StringArgumentType.getString(context, "id")
                                sendGraph(context.source.sender, id, management)
                                Command.SINGLE_SUCCESS
                            },
                    ),
            ),
    )

private fun infoCommand(
    permission: String,
    type: ModuleType,
    management: ModuleManagementService,
) = Commands.literal("info")
    .requiresPermission(permission)
    .then(
        Commands.argument("id", StringArgumentType.word())
            .suggestsModuleIds(type, management)
            .executes { context ->
                sendInfo(
                    context.source.sender,
                    StringArgumentType.getString(context, "id"),
                    type,
                    management,
                )
                Command.SINGLE_SUCCESS
            },
    )

private fun LiteralArgumentBuilder<CommandSourceStack>.requiresPermission(permission: String) =
    requires { source -> source.sender.hasPermission(permission) }

private fun RequiredArgumentBuilder<CommandSourceStack, String>.suggestsModuleIds(
    type: ModuleType?,
    management: ModuleManagementService,
) = suggests { _, builder ->
    management.snapshots()
        .filter { type == null || it.descriptor.type == type }
        .forEach { builder.suggest(it.descriptor.id) }
    builder.buildFuture()
}

private fun sendInfo(
    sender: CommandSender,
    id: String,
    type: ModuleType,
    management: ModuleManagementService,
) {
    val inspection = management.inspect(id)?.takeIf { it.snapshot.descriptor.type == type }
    sender.sendMessage(inspection?.let(::getModuleInfoMessage) ?: getUnknownModuleMessage(id))
}

private fun sendGraph(sender: CommandSender, id: String, management: ModuleManagementService) {
    val paths = management.dependencyPaths(id)
    sender.sendMessage(if (paths.isEmpty()) getUnknownModuleMessage(id) else getModuleGraphMessage(paths))
}
