package io.github.metrolung.traitorgame;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.PlayerProfileListResolver;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;

public class VoteCommand {
    private final TraitorGame plugin;

    public VoteCommand(TraitorGame plugin) {
        this.plugin = plugin;
    }

    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("vote")
            .then(Commands.literal("skip")
                .executes(this::executeSkip)
            )
            .then(Commands.literal("end")
                .executes(this::executeVoteEnd)
            )
            .then(Commands.argument("player", StringArgumentType.word())
                .executes(this::executeVote)
            );
    }

    private int executeVote(CommandContext<CommandSourceStack> ctx) {
        var playerName = ctx.getArgument("player", String.class);

        if (!(ctx.getSource().getExecutor() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage("Only players can vote");
            return Command.SINGLE_SUCCESS;
        }

        if (plugin.getSessionManager().getSession() == null) {
            ctx.getSource().getSender().sendMessage("No active session");
            return Command.SINGLE_SUCCESS;
        }

        ctx.getSource().getSender().sendMessage(plugin.getSessionManager().getSession().onVote(player, playerName));

        return Command.SINGLE_SUCCESS;
    }

    private int executeSkip(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getExecutor() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage("Only players can vote");
            return Command.SINGLE_SUCCESS;
        }

        if (plugin.getSessionManager().getSession() == null) {
            ctx.getSource().getSender().sendMessage("No active session");
            return Command.SINGLE_SUCCESS;
        }

        ctx.getSource().getSender().sendMessage(plugin.getSessionManager().getSession().onSkip(player));

        return Command.SINGLE_SUCCESS;
    }

    private int executeVoteEnd(CommandContext<CommandSourceStack> ctx) {
        if (!(ctx.getSource().getExecutor() instanceof Player player)) {
            ctx.getSource().getSender().sendMessage("Only players can vote");
            return Command.SINGLE_SUCCESS;
        }

        if (plugin.getSessionManager().getSession() == null) {
            ctx.getSource().getSender().sendMessage("No active session");
            return Command.SINGLE_SUCCESS;
        }

        ctx.getSource().getSender().sendMessage(plugin.getSessionManager().getSession().onVoteEndGame(player));

        return Command.SINGLE_SUCCESS;
    }
}