package io.github.metrolung.traitorgame

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.NamespacedKey
import org.bukkit.plugin.java.JavaPlugin

class TraitorGamePlugin : JavaPlugin() {
    val sessionManager: SessionManager = SessionManager()

    override fun onEnable() {
        saveDefaultConfig()

        this.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            event.registrar().register(TraitorGameCommand(this).create().build())
            event.registrar().register(RoleCommand(this).create().build())
            event.registrar().register(VoteCommand(this).create().build())
            event.registrar().register(BackCommand(this).create().build())
            event.registrar().register(HowToPlayCommand.create().build())
        }

        server.pluginManager.registerEvents(sessionManager, this)
    }

    override fun onDisable() {
        sessionManager.endSession()
    }

    companion object {
        @JvmField
        val namespace = "traitorgame"

        fun key(s: String) = NamespacedKey(namespace, s)
    }
}
