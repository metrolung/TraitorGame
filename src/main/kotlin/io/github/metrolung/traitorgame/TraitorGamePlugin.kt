package io.github.metrolung.traitorgame

import io.github.metrolung.traitorgame.commands.BackCommand
import io.github.metrolung.traitorgame.commands.InfoCommand
import io.github.metrolung.traitorgame.commands.RoleCommand
import io.github.metrolung.traitorgame.commands.ShoutCommand
import io.github.metrolung.traitorgame.commands.TraitorGameCommand
import io.github.metrolung.traitorgame.commands.VoteCommand
import io.github.metrolung.traitorgame.role.roles.Jester
import io.github.metrolung.traitorgame.role.roles.Mogul
import io.github.metrolung.traitorgame.role.roles.Parasite
import io.github.metrolung.traitorgame.role.roles.Survivor
import io.github.metrolung.traitorgame.role.roles.Traitor
import io.github.metrolung.traitorgame.trait.Detective
import io.github.metrolung.traitorgame.trait.LastChance
import io.github.metrolung.traitorgame.trait.Pacifist
import io.github.metrolung.traitorgame.trait.Sheriff
import io.github.metrolung.traitorgame.trait.Trait
import io.github.metrolung.traitorgame.trait.Victim
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.NamespacedKey
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class TraitorGamePlugin : JavaPlugin() {
    val sessionManager: SessionManager = SessionManager()

//    val mogul = roleManager.registerRole(Mogul.SETTINGS)
//    val detective = roleManager.registerRole(Detective.SETTINGS)
//    val astral = roleManager.registerRole(Astral.SETTINGS)
//    val jester = roleManager.registerRole(Jester.SETTINGS)

    override fun reloadConfig() {
        super.reloadConfig()
        TraitRegistry.updateConfigs()
        RoleRegistry.updateConfigs()
    }

    override fun onEnable() {
        saveResource("config.yml", false)
        saveResource("roles.yml", false)
        saveResource("traits.yml", false)

        TraitRegistry.setConfigurationFile(this, File(dataFolder, "traits.yml"))
        RoleRegistry.setConfigurationFile(this, File(dataFolder, "roles.yml"))

        Survivor.SETTINGS.register(RoleRegistry)
        Traitor.SETTINGS.register(RoleRegistry)
        Mogul.SETTINGS.register(RoleRegistry)
        Jester.SETTINGS.register(RoleRegistry)
        Parasite.SETTINGS.register(RoleRegistry)

        Pacifist.SETTINGS.register(TraitRegistry)
        Sheriff.SETTINGS.register(TraitRegistry)
        LastChance.SETTINGS.register(TraitRegistry)
        Detective.SETTINGS.register(TraitRegistry)
        Victim.SETTINGS.register(TraitRegistry)

        RoleRegistry.updateConfigs()
        TraitRegistry.updateConfigs()

        val traitSet = sortedSetOf<Trait>(Comparator { a, b ->
            if (a.settings.priority == b.settings.priority) {
                return@Comparator b.settings.key.compareTo(a.settings.key)
            }
            b.settings.priority.compareTo(a.settings.priority)
        })

        this.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            event.registrar().register(TraitorGameCommand(this).create().build())
            event.registrar().register(RoleCommand(this).create().build())
            event.registrar().register(VoteCommand(this).create().build())
            event.registrar().register(BackCommand(this).create().build())
            event.registrar().register(InfoCommand.create().build())
            event.registrar().register(ShoutCommand.create().build())
        }
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
