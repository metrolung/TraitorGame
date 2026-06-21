package io.github.metrolung.traitorgame

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.entity.Player

object HowToPlayCommand {
    fun create(): LiteralArgumentBuilder<CommandSourceStack> {
        return Commands.literal("howtoplay")
            .executes { ctx -> execute(ctx) }
    }

    private fun execute(ctx: CommandContext<CommandSourceStack>): Int {
        ctx.source.executor?.sendMessage(MiniMessage.deserialize("""
            
            <b>HOW TO PLAY:</b>
            It's a social deduction game. There's traitors and there are survivors.
            Your goal is to not die.
            
            <b>HOW TO WIN THE GAME:</b>
            The game will only end under any of these conditions:
            - Only 1 player alive
            - Players vote to end the game
            - The enderdragon is defeated (guaranteed survivor win)
            
        """.trimIndent()))


        return Command.SINGLE_SUCCESS
    }
}