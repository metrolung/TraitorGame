package io.github.metrolung.traitorgame;

import io.github.metrolung.traitorgame.RoleCommand;
import io.github.metrolung.traitorgame.SessionManager;
import io.github.metrolung.traitorgame.TraitorGameCommand;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

public final class TraitorGame extends JavaPlugin {
    private final SessionManager sessionManager = new SessionManager();

    @Override
    public void onEnable() {
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(new TraitorGameCommand(this).create().build());
            commands.registrar().register(new RoleCommand(this).create().build());
            commands.registrar().register(new VoteCommand(this).create().build());
        });

        getServer().getPluginManager().registerEvents(sessionManager, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }
}
