package io.github.metrolung.traitorgame

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.entity.Player

class BackCommand(private val plugin: TraitorGamePlugin) {
    fun create(): LiteralArgumentBuilder<CommandSourceStack> {
        return Commands.literal("back")
            .executes { ctx -> this.executeBack(ctx) }
    }

    private fun executeBack(ctx: CommandContext<CommandSourceStack>): Int {
        val executor = ctx.source.executor as? Player ?: run {
            ctx.source.sender.sendPlainMessage("Executor must be player")
            return Command.SINGLE_SUCCESS
        }

        val session = plugin.sessionManager.session ?: run {
            ctx.source.sender.sendPlainMessage("No active session")
            return Command.SINGLE_SUCCESS
        }

        ctx.source.sender.sendMessage(session.sendBack(executor.uniqueId))

        return Command.SINGLE_SUCCESS
    }
}