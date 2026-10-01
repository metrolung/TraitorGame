package io.github.metrolung.traitorgame

import io.github.metrolung.traitorgame.commands.BackCommand
import io.github.metrolung.traitorgame.commands.InfoCommand
import io.github.metrolung.traitorgame.commands.RoleCommand
import io.github.metrolung.traitorgame.commands.ShoutCommand
import io.github.metrolung.traitorgame.commands.TraitorGameCommand
import io.github.metrolung.traitorgame.commands.VoteCommand
import io.github.metrolung.traitorgame.role.RoleManager
import io.github.metrolung.traitorgame.role.roles.Astral
import io.github.metrolung.traitorgame.role.roles.Detective
import io.github.metrolung.traitorgame.role.roles.Jester
import io.github.metrolung.traitorgame.role.roles.Mogul
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.NamespacedKey
import org.bukkit.plugin.java.JavaPlugin

class TraitorGamePlugin : JavaPlugin() {
    val sessionManager: SessionManager = SessionManager()

    val roleManager = RoleManager()
    val mogul = roleManager.registerRole(Mogul.SETTINGS)
    val detective = roleManager.registerRole(Detective.SETTINGS)
    val astral = roleManager.registerRole(Astral.SETTINGS)
    val jester = roleManager.registerRole(Jester.SETTINGS)

    override fun onEnable() {
        saveDefaultConfig()
        afterConfigLoaded()

        this.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            event.registrar().register(TraitorGameCommand(this).create().build())
            event.registrar().register(RoleCommand(this).create().build())
            event.registrar().register(VoteCommand(this).create().build())
            event.registrar().register(BackCommand(this).create().build())
            event.registrar().register(InfoCommand.create().build())
            event.registrar().register(ShoutCommand.create().build())
        }
    }

    fun afterConfigLoaded() {
        mogul.setFixed(
            config.getInt("role.mogul.count"),
            config.getDouble("role.mogul.chance")
        )
        detective.setFixed(
            config.getInt("role.detective.count"),
            config.getDouble("role.detective.chance")
        )
        astral.setFixed(
            config.getInt("role.astral.count"),
            config.getDouble("role.astral.chance")
        )
        jester.setFixed(
            config.getInt("role.jester.count"),
            config.getDouble("role.jester.chance")
        )
    }

    override fun onDisable() {
        sessionManager.endSession()
    }

    companion object {
        const val NAMESPACE = "traitorgame"

        fun key(s: String) = NamespacedKey(NAMESPACE, s)
    }

    fun getCodewords(): List<String> {
        val codewords = getResource("codewords.txt") ?: return listOf("sus")
        val reader = codewords.bufferedReader(Charsets.UTF_8)
        return reader.readLines()
    }
}
