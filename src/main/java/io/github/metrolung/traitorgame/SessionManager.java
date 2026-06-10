package io.github.metrolung.traitorgame;

import org.bukkit.Server;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;

public class SessionManager {
    private Session oldSession;
    private Session session;

    public SessionManager() {}

    public void startSession(Server server, Plugin plugin, int traitorCount) {
        if (this.session == null) {
            this.session = new Session(server, plugin, this, traitorCount);
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
}
