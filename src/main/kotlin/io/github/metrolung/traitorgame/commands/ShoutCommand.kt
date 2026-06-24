package io.github.metrolung.traitorgame.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.github.metrolung.traitorgame.Colors
import io.github.metrolung.traitorgame.MiniMessage
import io.github.metrolung.traitorgame.TraitorGamePlugin
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.bukkit.entity.Player

object ShoutCommand {
    fun create(): LiteralArgumentBuilder<CommandSourceStack> {
        return Commands.literal("back")
            .requires { src -> src.sender.isOp }
            .then(
                Commands.argument("message", StringArgumentType.greedyString())
                    .executes { ctx -> this.executeShout(ctx) }
            )
    }

    private fun executeShout(ctx: CommandContext<CommandSourceStack>): Int {
        val message = ctx.getArgument("message", String::class.java)

        ctx.source.sender.server.sendMessage(Component.empty())
        ctx.source.sender.server.sendMessage(MiniMessage.deserialize(message).color(TextColor.color(Colors.VERY_YELLOW)))
        ctx.source.sender.server.sendMessage(Component.empty())

        return Command.SINGLE_SUCCESS
    }
}