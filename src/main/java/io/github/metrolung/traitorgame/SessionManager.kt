package io.github.metrolung.traitorgame;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;

public class SessionManager implements Listener {
    private Session oldSession;
    private Session session;

    public SessionManager() {}

    public void startSession(Server server, Plugin plugin, SessionConfigs configs) {
        if (this.session == null) {
            this.session = new Session(server, plugin, this, configs);
            this.session.onSessionStart();
        } else {
            throw new RuntimeException("Session already active");
        }
    }

    public void endSession(Session session) {
        if (this.session == session) {
            this.oldSession = this.session;
            this.session = null;
            session.onSessionEnd();
        }
    }

    public void endSession() {
        this.endSession(this.getSession());
    }

    public boolean isSessionActive() {
        return session != null;
    };

    public @Nullable Session getSession() {
        return session;
    }

    public @Nullable Session getOldSession() {
        return oldSession;
    }


    @EventHandler
    private void onServerTicked(ServerTickEndEvent event) {
        if (session != null) {
            session.onServerTicked();
        }
    }

    @EventHandler
    private void onPlayerDeath(PlayerDeathEvent death) {
        if (session != null) {
            session.onPlayerDeath(death.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (session != null) {
            if (event.getClickedBlock() != null) {
                session.onPlayerInteractWithBlock(event.getPlayer(), event.getClickedBlock().getLocation().toBlockLocation());
            }
        }
    }

    @EventHandler
    private void onBlockBroken(BlockBreakEvent event) {
        if (session != null) {
            if (session.onBlockRemoved(event.getBlock().getLocation().toBlockLocation())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    private void onBlockBroken(BlockExplodeEvent event) {
        if (session != null) {
            if (session.onBlockRemoved(event.getBlock().getLocation().toBlockLocation())) {
                event.setCancelled(true);
            }
        }
    }
}
