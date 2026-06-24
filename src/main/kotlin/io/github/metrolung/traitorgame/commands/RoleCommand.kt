package io.github.metrolung.traitorgame.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.github.metrolung.traitorgame.TraitorGamePlugin
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

class RoleCommand(private val plugin: TraitorGamePlugin) {
    fun create(): LiteralArgumentBuilder<CommandSourceStack> {
        return Commands.literal("role")
            .executes { ctx -> this.executeRole(ctx!!) }
            .then(
                Commands.argument("target", ArgumentTypes.player())
                    .requires { source -> source.sender.isOp }
                    .executes { ctx -> this.executeRoleOther(ctx) }
            )
    }

    private fun executeRole(ctx: CommandContext<CommandSourceStack>): Int {
        val executor = ctx.source.executor as? Player ?: run {
            ctx.source.sender.sendPlainMessage("Executor must be player")
            return Command.SINGLE_SUCCESS
        }

        return getRole(ctx.source, executor)
    }

    private fun executeRoleOther(ctx: CommandContext<CommandSourceStack>): Int {
        val targetResolver = ctx.getArgument("target", PlayerSelectorArgumentResolver::class.java)
        val target = targetResolver.resolve(ctx.source).first()

        return getRole(ctx.source, target)
    }

    private fun getRole(source: CommandSourceStack, player: Player): Int {
        val role = plugin.sessionManager.session?.getRole(player) ?: run {
            source.sender.sendPlainMessage("Could not determine role of ${player.name}")
            return Command.SINGLE_SUCCESS
        }

        source.sender.sendMessage(
            player.name().append(
                Component.text(" has role: ").append(
                    role.stylized
                )
            )
        )

        return Command.SINGLE_SUCCESS
    }
}