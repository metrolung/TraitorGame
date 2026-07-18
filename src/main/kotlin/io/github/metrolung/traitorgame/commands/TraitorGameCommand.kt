package io.github.metrolung.traitorgame.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.DoubleArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.github.metrolung.traitorgame.SessionSettings
import io.github.metrolung.traitorgame.TraitorGamePlugin
import io.github.metrolung.traitorgame.WorldBorderHelper
import io.github.metrolung.traitorgame.component
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player

class TraitorGameCommand(private val plugin: TraitorGamePlugin) {
    fun create(): LiteralArgumentBuilder<CommandSourceStack> {
        return Commands.literal("traitorgame")
            .requires { source -> source.sender.isOp }
            .then(Commands.literal("start")
                .then(Commands.argument("traitorcount", IntegerArgumentType.integer(0))
                    .then(Commands.argument("passiveneutralcount", IntegerArgumentType.integer(0))
                        .then(Commands.argument("evilneutralcount", IntegerArgumentType.integer(0))
                            .executes { ctx -> this.executeStart(
                                ctx,
                                ctx.getArgument("traitorcount", Int::class.javaPrimitiveType),
                                ctx.getArgument("passiveneutralcount", Int::class.javaPrimitiveType),
                                ctx.getArgument("evilneutralcount", Int::class.javaPrimitiveType),
                            ) }
                        )
                    )
                    .then(Commands.argument("neutralcount", IntegerArgumentType.integer(0))
                        .executes { ctx ->
                            ctx.source.sender.sendPlainMessage("wrong route")
                            val neutrals = ctx.getArgument("neutralcount", Int::class.javaPrimitiveType)
                            val passiveNeutrals = (0..neutrals).random()
                            val evilNeutrals = neutrals - passiveNeutrals

                            this.executeStart(
                                ctx,
                                ctx.getArgument("traitorcount", Int::class.javaPrimitiveType),
                                passiveNeutrals,
                                evilNeutrals
                            )
                        }
                    )
                )
            )
            .then(Commands.literal("revive")
                .then(Commands.argument("target", ArgumentTypes.players())
                    .executes { ctx -> this.executeRevive(
                        ctx,
                        ctx.getArgument("target", PlayerSelectorArgumentResolver::class.java).resolve(ctx.source).asIterable(),
                        null,
                    ) }
                    .then(Commands.argument("role", ArgumentTypes.namespacedKey())
                        .suggests { context, builder ->
                            for (key in plugin.roles.keys) {
                                builder.suggest(key.toString())
                            }
                            builder.buildFuture()
                        }
                        .executes { ctx -> this.executeRevive(
                            ctx,
                            ctx.getArgument("target", PlayerSelectorArgumentResolver::class.java).resolve(ctx.source).asIterable(),
                            ctx.getArgument("role", NamespacedKey::class.java),
                        ) }
                    )
                )
            )
            .then(Commands.literal("end")
                .executes { ctx -> this.executeEnd(ctx) }
            )
            .then(Commands.literal("wbhelper")
                .then(Commands.argument("diameter", DoubleArgumentType.doubleArg(0.0))
                    .executes { ctx -> this.executeWbhelper(ctx, ctx.getArgument("diameter", Double::class.javaPrimitiveType)) }
                )
            )
            .then(Commands.literal("forcemeeting")
                .executes { ctx -> this.executeForceMeeting(ctx) }
            )
            .then(Commands.literal("result")
                .executes { ctx -> this.executeResult(ctx) }
            )
            .then(Commands.literal("reloadconfigs")
                .executes { ctx -> this.executeReloadConfigs(ctx) }
            )
    }
    
    private fun executeRevive(ctx: CommandContext<CommandSourceStack>, players: Iterable<Player>, roleKey: NamespacedKey?): Int {
        val session = plugin.sessionManager.session ?: run {
            ctx.source.sender.sendPlainMessage("No active session")
            return Command.SINGLE_SUCCESS
        }

        val role = if (roleKey == null) {
            plugin.roles[TraitorGamePlugin.key("survivor")]!!
        } else {
            plugin.roles[roleKey] ?: plugin.roles[TraitorGamePlugin.key("survivor")]!!
        }

        for (player in players) {
            val result = session.revive(player, role.builder)
            ctx.source.sender.sendMessage(result.component)
        }
        return Command.SINGLE_SUCCESS
    }

    private fun executeStart(
        ctx: CommandContext<CommandSourceStack>,
        traitorCount: Int,
        passiveNeutralCount: Int,
        evilNeutralCount: Int
    ): Int {
        if (plugin.sessionManager.isSessionActive) {
            ctx.source.sender.sendPlainMessage("Session currently active")
            return Command.SINGLE_SUCCESS
        }

        plugin.sessionManager.startSession(
            plugin.server, plugin, SessionSettings.create(
                plugin,
                traitorCount,
                passiveNeutralCount,
                evilNeutralCount,
                ctx.source.location.toBlockLocation()
            )
        )

        return Command.SINGLE_SUCCESS
    }

    private fun executeEnd(ctx: CommandContext<CommandSourceStack>): Int {
        plugin.sessionManager.endSession()

        return Command.SINGLE_SUCCESS
    }


    private fun executeWbhelper(ctx: CommandContext<CommandSourceStack>, diameter: Double): Int {
        val result = WorldBorderHelper.setupWorldBorderAsync(plugin, ctx.source.sender.server, ctx.source.location, diameter)

        ctx.source.sender.sendPlainMessage(result)

        return Command.SINGLE_SUCCESS
    }

    private fun executeReloadConfigs(ctx: CommandContext<CommandSourceStack>): Int {
        plugin.reloadConfig()

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