package io.github.metrolung.traitorgame

import io.github.metrolung.traitorgame.commands.BackCommand
import io.github.metrolung.traitorgame.commands.InfoCommand
import io.github.metrolung.traitorgame.commands.RoleCommand
import io.github.metrolung.traitorgame.commands.ShoutCommand
import io.github.metrolung.traitorgame.commands.TraitorGameCommand
import io.github.metrolung.traitorgame.commands.VoteCommand
import io.github.metrolung.traitorgame.role.Role
import io.github.metrolung.traitorgame.role.RoleAlignment
import io.github.metrolung.traitorgame.role.roles.Astral
import io.github.metrolung.traitorgame.role.roles.Detective
import io.github.metrolung.traitorgame.role.roles.Jester
import io.github.metrolung.traitorgame.role.roles.Mogul
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.plugin.java.JavaPlugin

class TraitorGamePlugin : JavaPlugin() {
    val sessionManager: SessionManager = SessionManager()

    val roles: MutableMap<NamespacedKey, Role.Setting> = mutableMapOf(
        key("traitor") to Role.Setting(
            0,
            0.0,
            RoleAlignment.TRAITOR,
            ::Jester
        ),

        key("survivor") to Role.Setting(
            0,
            0.0,
            RoleAlignment.SURVIVOR,
            ::Jester
        ),

        key("jester") to Role.Setting(
            config.getInt("role.jester.count"),
            config.getDouble("role.jester.chance")/100.0,
            RoleAlignment.EVIL_NEUTRAL,
            ::Jester
        ),

        key("detective") to Role.Setting(
            config.getInt("role.detective.count"),
            config.getDouble("role.detective.chance")/100.0,
            RoleAlignment.SURVIVOR,
            ::Detective
        ),

        key("astral") to Role.Setting(
            config.getInt("role.astral.count"),
            config.getDouble("role.astral.chance")/100.0,
            RoleAlignment.SURVIVOR,
            ::Astral
        ),

        key("mogul") to Role.Setting(
            config.getInt("role.mogul.count"),
            config.getDouble("role.mogul.chance")/100.0,
            RoleAlignment.PASSIVE_NEUTRAL,
            ::Mogul
        ),
    )

    override fun onEnable() {
        saveDefaultConfig()

        this.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            event.registrar().register(TraitorGameCommand(this).create().build())
            event.registrar().register(RoleCommand(this).create().build())
            event.registrar().register(VoteCommand(this).create().build())
            event.registrar().register(BackCommand(this).create().build())
            event.registrar().register(InfoCommand.create().build())
            event.registrar().register(ShoutCommand.create().build())
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

    fun getCodewords(): List<String> {
        val codewords = getResource("codewords.txt") ?: return listOf("sus")
        val reader = codewords.bufferedReader(Charsets.UTF_8)
        return reader.readLines()
    }
}
