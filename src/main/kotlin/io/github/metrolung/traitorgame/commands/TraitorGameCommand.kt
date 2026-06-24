package io.github.metrolung.traitorgame.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.github.metrolung.traitorgame.SessionSettings
import io.github.metrolung.traitorgame.TraitorGamePlugin
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.entity.Player

class TraitorGameCommand(private val plugin: TraitorGamePlugin) {
    fun create(): LiteralArgumentBuilder<CommandSourceStack> {
        return Commands.literal("traitorgame")
            .requires { source -> source.sender.isOp }
            .then(
                Commands.literal("start")
                .then(
                    Commands.argument("traitorcount", IntegerArgumentType.integer(0))
                    .then(
                        Commands.argument("detectivecount", IntegerArgumentType.integer(0))
                        .then(
                            Commands.argument("neutralcount", IntegerArgumentType.integer(0))
                            .executes { ctx -> this.executeStart(
                                ctx,
                                ctx.getArgument("traitorcount", Int::class.javaPrimitiveType),
                                ctx.getArgument("detectivecount", Int::class.javaPrimitiveType),
                                ctx.getArgument("neutralcount", Int::class.javaPrimitiveType),
                            ) }
                        )
                    )
                )
            )
            .then(
                Commands.literal("end")
                .executes { ctx -> this.executeEnd(ctx) }
            )
            .then(
                Commands.literal("forcemeeting")
                .executes { ctx -> this.executeForceMeeting(ctx) }
            )
            .then(
                Commands.literal("result")
                .executes { ctx -> this.executeResult(ctx) }
            )
    }

    private fun executeStart(
        ctx: CommandContext<CommandSourceStack>,
        traitorCount: Int,
        detectiveCount: Int,
        neutralCount: Int
    ): Int {
        if (plugin.sessionManager.isSessionActive) {
            ctx.source.sender.sendPlainMessage("Session currently active")
            return Command.SINGLE_SUCCESS
        }

        plugin.sessionManager.startSession(
            plugin.server, plugin, SessionSettings.create(
                plugin,
                traitorCount,
                detectiveCount,
                neutralCount,
                ctx.source.location.toBlockLocation()
            )
        )

        return Command.SINGLE_SUCCESS
    }

    private fun executeEnd(ctx: CommandContext<CommandSourceStack>): Int {
        plugin.sessionManager.endSession()

        return Command.SINGLE_SUCCESS
    }

    private fun executeForceMeeting(ctx: CommandContext<CommandSourceStack>): Int {
        val session = plugin.sessionManager.session ?: run {
            ctx.source.sender.sendMessage("No active session")
            return Command.SINGLE_SUCCESS
        }

        session.startMeeting()

        return Command.SINGLE_SUCCESS
    }

    private fun executeResult(ctx: CommandContext<CommandSourceStack>): Int {
        val oldSession = plugin.sessionManager.oldSession

        val executor = ctx.source.executor as? Player ?: run {
            ctx.source.sender.sendPlainMessage("Executor must be player")
            return Command.SINGLE_SUCCESS
        }

        if (oldSession == null) {
            ctx.source.sender.sendPlainMessage("No previous session")
            return Command.SINGLE_SUCCESS
        }

        val endGameText = oldSession.endGameText
        if (endGameText == null) {
            ctx.source.sender.sendPlainMessage("Could not retrieve end game message")
            return Command.SINGLE_SUCCESS
        }

        for (line in endGameText) {
            executor.sendMessage(line)
        }

        return Command.SINGLE_SUCCESS
    }
}