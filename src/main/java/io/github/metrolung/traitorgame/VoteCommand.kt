package io.github.metrolung.traitorgame

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.entity.Player

class VoteCommand(private val plugin: TraitorGame) {
    fun create(): LiteralArgumentBuilder<CommandSourceStack> {
        return Commands.literal("vote")
            .then(
                Commands.literal("skip")
                    .executes { ctx -> this.executeSkip(ctx) }
            )
            .then(
                Commands.literal("end")
                    .executes { ctx -> this.executeVoteEnd(ctx) }
            )
            .then(
                Commands.literal("player").then(
                    Commands.argument("player", StringArgumentType.word())
                        .executes { ctx -> this.executeVote(ctx) }
                )
            )
            .then(
                Commands.argument("player", StringArgumentType.word())
                    .executes { ctx -> this.executeVote(ctx) }
            )
    }

    private fun executeVote(ctx: CommandContext<CommandSourceStack>): Int {
        val playerName = ctx.getArgument("player", String::class.java)

        val executor = ctx.source.executor as? Player ?: run {
            ctx.source.sender.sendPlainMessage("Executor must be player")
            return Command.SINGLE_SUCCESS
        }

        val session = plugin.sessionManager.session ?: run {
            ctx.source.sender.sendMessage("No active session")
            return Command.SINGLE_SUCCESS
        }

        val voted = executor.server.getOfflinePlayer(playerName)
        ctx.source.sender.sendMessage(session.onVote(executor, Vote.PlayerVote(voted.uniqueId)))

        return Command.SINGLE_SUCCESS
    }

    private fun executeSkip(ctx: CommandContext<CommandSourceStack>): Int {
        val executor = ctx.source.executor as? Player ?: run {
            ctx.source.sender.sendPlainMessage("Executor must be player")
            return Command.SINGLE_SUCCESS
        }

        val session = plugin.sessionManager.session ?: run {
            ctx.source.sender.sendMessage("No active session")
            return Command.SINGLE_SUCCESS
        }

        ctx.source.sender.sendMessage(session.onVote(executor, Vote.Skip))

        return Command.SINGLE_SUCCESS
    }

    private fun executeVoteEnd(ctx: CommandContext<CommandSourceStack>): Int {
        val executor = ctx.source.executor as? Player ?: run {
            ctx.source.sender.sendPlainMessage("Executor must be player")
            return Command.SINGLE_SUCCESS
        }

        val session = plugin.sessionManager.session ?: run {
            ctx.source.sender.sendMessage("No active session")
            return Command.SINGLE_SUCCESS
        }

        ctx.source.sender.sendMessage(session.onVote(executor, Vote.EndGame))

        return Command.SINGLE_SUCCESS
    }
}