package io.github.metrolung.traitorgame.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands

object InfoCommand {
    val info: Map<String, String> = mapOf(
        "howtoplay" to """
            It's a run-of-the-mill social deduction game.
            Depending on your role, you'll have different goals.
            All in all though, try not to die.
        """.trimIndent(),

        "corpse" to """
            When a player dies, they will drop dead into a corpse.
            It is not recommended that you touch the corpse, as you will contaminate it.
        """.trimIndent(),

        "survivor" to """
            This role is pretty basic.
            You're just trying to beat the dragon.
            You can also win by voting to end the game, but that is risky.
            If any traitors are alive when you vote to end the game, you will lose.
        """.trimIndent(),

        "traitor" to """
            You are trying to get all of the players to vote to end the game.
            There are two main ways of doing this:
             - Trick players into believing the survivors are dead
             - Kill all the survivors and just do it yourself
            You can identify your fellow traitors through your code word, or by showing your "manifesto" item.
        """.trimIndent(),

        "jester" to """
            You are considered an evil neutral role.
            The only way for you to win is by getting killed by a non evil role.
        """.trimIndent(),

        "mogul" to """
            You're just in it for the money.
            You are equipped with a box full of various merchandise.
            This merchandise can be used to bribe other players into giving you minerals.
            Putting minerals inside your money bag will increase your total worth.
            <click:suggest_command:/info worth>Use <yellow>/info worth</yellow> to get the values of several minerals.</click>
        """.trimIndent(),

        "worth" to """
            Value of some minerals. This list is not exhaustive.
             - Netherite: $250
             - Diamond: $40
             - Iron: $10
             - Gold: $5
             - Emerald: $1
             - Copper: $0.10
             - Amethyst: $0.01
        """.trimIndent(),

        "astral" to """
            Your role is pretty basic like the survivor.
            However, you can see ghosts (make sure your particles are on).
            This can be used in tandem with the detective to help find corpses.
        """.trimIndent(),

        "detective" to """
            You are equipped with various tools to find the traitors and track them down.
            <click:suggest_command:/info swab>Use <yellow>/info swab</yellow> for more info on the swab tools.</click>
            <click:suggest_command:/info notebook>Use <yellow>/info notebook</yellow> for more info on the notebook.</click>
        """.trimIndent(),

        "swab" to """
            Right click a player or a corpse with a swab to gather some DNA.
            Corpse swabs may have the DNA of any contaminator.
            Put the two swabs in the swab checker and then right click the swab checker to get a result.
            A match is indicative that the player has been near the corpse or maybe even killed it.
            An inconclusive result means its possible the corpse swab got the wrong DNA, or the player never touched the corpse.
        """.trimIndent(),

        "notebook" to """
            Shift right click the notebook to open it.
            Right click the notebook to log information about your surroundings.
            Right click a corpse with the notebook to get information about its death.
            The notebook will automatically log meeting results.
        """.trimIndent()
    )

    fun create(): LiteralArgumentBuilder<CommandSourceStack> {
        return Commands.literal("info")
            .executes { ctx -> execute(ctx) }
            .then(Commands.argument("infotype", StringArgumentType.word())
                .suggests { ctx, builder ->
                    for (key in info.keys) {
                        builder.suggest(key)
                    }
                    builder.buildFuture()
                }
                .executes { ctx -> executeInfo(ctx) })
    }

    private fun execute(ctx: CommandContext<CommandSourceStack>): Int {
        ctx.source.executor?.sendMessage("Use /info (infotype) for info on a specific subject")
        for (possibleInfo in info.keys) {
            ctx.source.executor?.sendMessage("- $possibleInfo")
        }

        return Command.SINGLE_SUCCESS
    }

    private fun executeInfo(ctx: CommandContext<CommandSourceStack>): Int {
        val infoType = ctx.getArgument("infotype", String::class.java)

        val info = info[infoType]

        if (info != null) {
            ctx.source.executor?.sendPlainMessage("")
            ctx.source.executor?.sendRichMessage(info)
            ctx.source.executor?.sendPlainMessage("")
        } else {
            ctx.source.sender.sendMessage("No matching info type")
        }

        return Command.SINGLE_SUCCESS
    }
}