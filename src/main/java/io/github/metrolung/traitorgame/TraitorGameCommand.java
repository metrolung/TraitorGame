package io.github.metrolung.traitorgame;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.github.metrolung.traitorgame.TraitorGame;

public class TraitorGameCommand {
    private final TraitorGame plugin;

    public TraitorGameCommand(TraitorGame plugin) {
        this.plugin = plugin;
    }

    public LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("traitorgame")
            .requires(sender -> sender.getSender().isOp())
            .then(Commands.literal("start")
                .then(Commands.argument("traitorcount", IntegerArgumentType.integer(0))
                    .executes(this::executeStart)
                )
            )
            .then(Commands.literal("end")
                .executes(this::executeEnd)
            )
            .then(Commands.literal("result")
                .executes(this::executeResult)
            );
    }

    private int executeStart(CommandContext<CommandSourceStack> ctx) {
        if (plugin.getSessionManager().isSessionActive()) {
            ctx.getSource().getSender().sendPlainMessage("Session currently active");
            return Command.SINGLE_SUCCESS;
        }

        int traitorCount = ctx.getArgument("traitorcount", int.class);

        plugin.getSessionManager().startSession(plugin.getServer(), plugin, new SessionConfigs(
                20*60*5,
                20*30,
                20*60,
                traitorCount,
                ctx.getSource().getLocation()
        ));

        return Command.SINGLE_SUCCESS;
    }

    private int executeEnd(CommandContext<CommandSourceStack> ctx) {
        plugin.getSessionManager().endSession();

        return Command.SINGLE_SUCCESS;
    }

    private int executeResult(CommandContext<CommandSourceStack> ctx) {
        var oldSession = plugin.getSessionManager().getOldSession();

        if (oldSession == null) {
            ctx.getSource().getSender().sendPlainMessage("No previous session");
            return Command.SINGLE_SUCCESS;
        }

        oldSession.displayRoles();

        return Command.SINGLE_SUCCESS;
    }
}