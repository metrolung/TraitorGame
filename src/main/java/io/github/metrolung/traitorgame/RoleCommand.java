package io.github.metrolung.traitorgame;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class RoleCommand {
    private final TraitorGame plugin;

    public RoleCommand(TraitorGame plugin) {
        this.plugin = plugin;
    }

    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("role")
            .executes(this::executeRole)
            .then(Commands.argument("player", ArgumentTypes.player())
                .requires(ctx -> ctx.getSender().isOp())
                .executes(this::executeRoleOther)
            );
    }

    private int executeRole(CommandContext<CommandSourceStack> ctx) {
        Entity executor = ctx.getSource().getExecutor();

        if (!(executor instanceof Player player)) {
            return Command.SINGLE_SUCCESS;
        }

        if (plugin.getSessionManager().getSession() == null) {
            return Command.SINGLE_SUCCESS;
        }

        Session.Role role = plugin.getSessionManager().getSession().getRole(player);
        if (role == null) {
            ctx.getSource().getSender().sendPlainMessage("%s has no role".formatted(player.name()));
            return Command.SINGLE_SUCCESS;
        }

        ctx.getSource().getSender().sendMessage(player.name().append(
            Component.text(" has role: ").append(
                role.getName()
            )
        ));

        return Command.SINGLE_SUCCESS;
    }

    private int executeRoleOther(CommandContext<CommandSourceStack> ctx) {
        Player player = ctx.getArgument("player", Player.class);

        if (plugin.getSessionManager().getSession() == null) {
            return Command.SINGLE_SUCCESS;
        }

        Session.Role role = plugin.getSessionManager().getSession().getRole(player);
        if (role == null) {
            ctx.getSource().getSender().sendPlainMessage("%s has no role".formatted(player.name()));
            return Command.SINGLE_SUCCESS;
        }

        ctx.getSource().getSender().sendMessage(player.name().append(
            Component.text(" has role: ").append(
                role.getName()
            )
        ));

        return Command.SINGLE_SUCCESS;
    }
}