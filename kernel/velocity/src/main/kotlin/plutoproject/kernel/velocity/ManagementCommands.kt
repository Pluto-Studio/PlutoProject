package plutoproject.kernel.velocity

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.velocitypowered.api.command.CommandSource
import kotlinx.coroutines.runBlocking
import plutoproject.kernel.api.ModuleType
import plutoproject.kernel.common.ModuleManagementService

internal fun createManagementCommand(
    management: ModuleManagementService,
): LiteralArgumentBuilder<CommandSource> = LiteralArgumentBuilder.literal<CommandSource>("plutoproject")
    .then(
        LiteralArgumentBuilder.literal<CommandSource>("feature")
            .then(
                LiteralArgumentBuilder.literal<CommandSource>("list")
                    .requiresPermission(FEATURE_LIST_PERMISSION)
                    .executes { context ->
                        val modules = management.snapshots().filter { it.descriptor.type == ModuleType.FEATURE }
                        context.source.sendMessage(getModuleListMessage("Feature", modules))
                        Command.SINGLE_SUCCESS
                    },
            )
            .then(infoCommand("info", FEATURE_INFO_PERMISSION, ModuleType.FEATURE, management))
            .then(
                LiteralArgumentBuilder.literal<CommandSource>("disable")
                    .requiresPermission(FEATURE_DISABLE_PERMISSION)
                    .then(
                        com.mojang.brigadier.builder.RequiredArgumentBuilder
                            .argument<CommandSource, String>("id", StringArgumentType.word())
                            .suggestsModuleIds(ModuleType.FEATURE, management)
                            .executes { context ->
                                val result = runBlocking {
                                    management.disable(StringArgumentType.getString(context, "id"))
                                }
                                context.source.sendMessage(getDisableResultMessage(result))
                                Command.SINGLE_SUCCESS
                            },
                    ),
            ),
    )
    .then(
        LiteralArgumentBuilder.literal<CommandSource>("capability")
            .then(
                LiteralArgumentBuilder.literal<CommandSource>("list")
                    .requiresPermission(CAPABILITY_LIST_PERMISSION)
                    .executes { context ->
                        val modules = management.snapshots().filter { it.descriptor.type == ModuleType.CAPABILITY }
                        context.source.sendMessage(getModuleListMessage("Capability", modules))
                        Command.SINGLE_SUCCESS
                    },
            )
            .then(infoCommand("info", CAPABILITY_INFO_PERMISSION, ModuleType.CAPABILITY, management)),
    )
    .then(
        LiteralArgumentBuilder.literal<CommandSource>("module")
            .then(
                LiteralArgumentBuilder.literal<CommandSource>("graph")
                    .requiresPermission(MODULE_GRAPH_PERMISSION)
                    .then(
                        com.mojang.brigadier.builder.RequiredArgumentBuilder
                            .argument<CommandSource, String>("id", StringArgumentType.word())
                            .suggestsModuleIds(null, management)
                            .executes { context ->
                                val id = StringArgumentType.getString(context, "id")
                                sendGraph(context.source, id, management)
                                Command.SINGLE_SUCCESS
                            },
                    ),
            ),
    )

private fun infoCommand(
    literal: String,
    permission: String,
    type: ModuleType,
    management: ModuleManagementService,
) = LiteralArgumentBuilder.literal<CommandSource>(literal)
    .requiresPermission(permission)
    .then(
        com.mojang.brigadier.builder.RequiredArgumentBuilder
            .argument<CommandSource, String>("id", StringArgumentType.word())
            .suggestsModuleIds(type, management)
            .executes { context ->
                sendInfo(context.source, StringArgumentType.getString(context, "id"), type, management)
                Command.SINGLE_SUCCESS
            },
    )

private fun LiteralArgumentBuilder<CommandSource>.requiresPermission(permission: String) =
    requires { source -> source.hasPermission(permission) }

private fun RequiredArgumentBuilder<CommandSource, String>.suggestsModuleIds(
    type: ModuleType?,
    management: ModuleManagementService,
) = suggests { _, builder ->
    management.snapshots()
        .filter { type == null || it.descriptor.type == type }
        .forEach { builder.suggest(it.descriptor.id) }
    builder.buildFuture()
}

private fun sendInfo(
    sender: CommandSource,
    id: String,
    type: ModuleType,
    management: ModuleManagementService,
) {
    val inspection = management.inspect(id)?.takeIf { it.snapshot.descriptor.type == type }
    sender.sendMessage(inspection?.let(::getModuleInfoMessage) ?: getUnknownModuleMessage(id))
}

private fun sendGraph(sender: CommandSource, id: String, management: ModuleManagementService) {
    val paths = management.dependencyPaths(id)
    sender.sendMessage(if (paths.isEmpty()) getUnknownModuleMessage(id) else getModuleGraphMessage(paths))
}
