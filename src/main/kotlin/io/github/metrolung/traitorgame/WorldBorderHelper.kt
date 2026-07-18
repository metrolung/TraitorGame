package io.github.metrolung.traitorgame

import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.Server
import org.bukkit.World
import org.bukkit.generator.structure.Structure
import kotlin.random.Random


object WorldBorderHelper {
    private fun getOverworld(server: Server) = server.getWorld(NamespacedKey.minecraft("overworld"))!!
    private fun getNether(server: Server) = server.getWorld(NamespacedKey.minecraft("the_nether"))!!

    private fun findStronghold(overworld: World, reference: Location, distance: Int): Location? {
        val result = overworld.locateNearestStructure(reference, Structure.STRONGHOLD, distance, false) ?: return null

        return result.location
    }

    private fun findFortress(nether: World, reference: Location, distance: Int): Location? {
        val result = nether.locateNearestStructure(reference, Structure.FORTRESS, distance, false) ?: return null

        return result.location
    }

    fun setupWorldBorderAsync(plugin: TraitorGamePlugin, server: Server, location: Location, diameter: Double): String {
        val netherLocation = location.clone().multiply(0.125)

        val nether = getNether(server)
        val overworld = getOverworld(server)
        overworld.worldBorder.size = 59_999_968.0
        nether.worldBorder.size = 59_999_968.0
        overworld.worldBorder.center = Location(overworld, 0.0, 0.0, 0.0)
        nether.worldBorder.center = Location(nether, 0.0, 0.0, 0.0)

        val searchDistance = plugin.config.getInt("structure-search-radius")
        val stronghold = findStronghold(overworld, location, searchDistance) ?: return "Couldn't find stronghold"
        val fortress = findFortress(nether, netherLocation, searchDistance) ?: return "Couldn't find fortress"

        val strongholdMargin = 50.0
        val fortressMargin = 75.0

        val overworldBorderCenter = stronghold.add(
            Random.nextDouble(strongholdMargin - diameter/2, diameter/2 - strongholdMargin),
            0.0,
            Random.nextDouble(strongholdMargin - diameter/2, diameter/2 - strongholdMargin)
        )


        val netherBorderCenter = fortress.add(
            Random.nextDouble(fortressMargin - diameter/2, diameter/2 - fortressMargin),
            0.0,
            Random.nextDouble(fortressMargin - diameter/2, diameter/2 - fortressMargin)
        )

        for (player in server.onlinePlayers) {
            val pos = overworldBorderCenter.clone()
            pos.x += Random.nextDouble(-5.0, 5.0)
            pos.y += Random.nextDouble(-5.0, 5.0)

            val highestY = overworld.getHighestBlockYAt(overworldBorderCenter)
            pos.y = highestY.toDouble()+1.0

            player.teleport(pos)
            player.sendPlainMessage("New spawn point assigned")
        }

        overworld.worldBorder.center = overworldBorderCenter
        nether.worldBorder.center = netherBorderCenter
        overworld.worldBorder.size = diameter
        nether.worldBorder.size = diameter

        overworld.spawnLocation = overworldBorderCenter

        return "Success"
    }

}

/*

public class WBHelperCommand {
    private static final Random random = new Random();

    private WBHelperCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("wbhelper")
            .requires(sender -> sender.getSender().isOp())
            .then(Commands.literal("setup")
                .then(Commands.argument("diameter", DoubleArgumentType.doubleArg(0))
                    .executes(WBHelperCommand::executeSetup)
                )
            );
    }

    private static int executeSetup(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        Location location = ctx.getSource().getLocation();
        World overworld = ctx.getSource().getSender().getServer().getWorld(NamespacedKey.minecraft("overworld"));
        World nether = ctx.getSource().getSender().getServer().getWorld(NamespacedKey.minecraft("the_nether"));
        double diameter = ctx.getArgument("diameter", double.class);
        double strongholdMargin = 50;
        double fortressMargin = 75;

        assert overworld != null;
        assert nether != null;

        WorldBorder overworldBorder = overworld.getWorldBorder();
        WorldBorder netherBorder = nether.getWorldBorder();
        overworldBorder.reset();
        netherBorder.reset();

        var stronghold = overworld.locateNearestStructure(location, Structure.STRONGHOLD, 1000, false);
        if (stronghold == null) {
            sender.sendPlainMessage("Could not find stronghold");
            return Command.SINGLE_SUCCESS;
        }

        var fortress = nether.locateNearestStructure(stronghold.getLocation(), Structure.FORTRESS, 1000, false);
        if (fortress == null) {
            sender.sendPlainMessage("Could not find fortress");
            return Command.SINGLE_SUCCESS;
        }

        var overworldBorderCenter = stronghold.getLocation().add(
            random.nextDouble(strongholdMargin - diameter/2, diameter/2 - strongholdMargin),
            0,
            random.nextDouble(strongholdMargin - diameter/2, diameter/2 - strongholdMargin)
        );
        var netherBorderCenter = fortress.getLocation().add(
            random.nextDouble(fortressMargin - diameter/2, diameter/2 - fortressMargin),
            0,
            random.nextDouble(fortressMargin - diameter/2, diameter/2 - fortressMargin)
        );

        overworldBorder.setSize(diameter);
        overworldBorder.setCenter(overworldBorderCenter);

        netherBorder.setSize(diameter);
        netherBorder.setCenter(netherBorderCenter);

        overworld.setSpawnLocation(overworldBorderCenter);

        sender.getServer().dispatchCommand(
            sender,
            String.format("execute in minecraft:overworld run spreadplayers %f %f 5 5 false @a",
                overworldBorderCenter.x(),
                overworldBorderCenter.z()
            )
        );

        return Command.SINGLE_SUCCESS;
    }

}


 */