package io.github.metrolung.traitorgame

import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.handler.LifecycleEventHandler
import io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.plugin.java.JavaPlugin

class TraitorGame : JavaPlugin() {
    val sessionManager: SessionManager = SessionManager()

    override fun onEnable() {
        saveDefaultConfig()

        this.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            event.registrar().register(TraitorGameCommand(this).create().build())
            event.registrar().register(RoleCommand(this).create().build())
            event.registrar().register(VoteCommand(this).create().build())
            event.registrar().register(BackCommand(this).create().build())
        }

        server.pluginManager.registerEvents(sessionManager, this)
    }

    override fun onDisable() {
        sessionManager.endSession()
    }

    companion object {
        @JvmField
        val commandNamespace = "traitorgame"
    }
}
