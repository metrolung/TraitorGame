package io.github.metrolung.traitorgame

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerPriority
import io.github.metrolung.traitorgame.role.RoleManager
import org.bukkit.Server
import org.bukkit.event.HandlerList
import org.bukkit.plugin.Plugin

class SessionManager {
    var oldSession: Session? = null
        private set
    var session: Session? = null
        private set
    val isSessionActive: Boolean
        get() = session != null

    fun startSession(server: Server, plugin: Plugin, roleManager: RoleManager, configs: SessionSettings) {
        if (this.session == null) {
            val session = Session(server, plugin, this, roleManager, configs)
            this.session = session
            session.start()

            val common = PacketEvents.getAPI().eventManager.registerListener(session, PacketListenerPriority.NORMAL)
            server.pluginManager.registerEvents(session, plugin)
            session.listenerCommon = common
        } else {
            error("Session already active")
        }
    }

    @JvmOverloads
    fun endSession(reason: EndGameReason? = null) {
        this.session?.let { session ->
            session.listenerCommon?.let { common -> PacketEvents.getAPI().eventManager.unregisterListener(common) }
            HandlerList.unregisterAll(session)
            this.oldSession = session
            this.session = null
            session.forceEndSession(reason)
            session.deinit()
        }
    }
}
