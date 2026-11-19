package io.github.metrolung.traitorgame.trait

import io.github.metrolung.traitorgame.Colors
import io.github.metrolung.traitorgame.TraitorGamePlugin
import io.github.metrolung.traitorgame.trait.event.TraitEvent
import org.bukkit.Material
import xyz.xenondevs.invui.item.ItemBuilder

class Pacifist : Trait {
    var punishmentTime = 0

    override fun onDealDamage(event: TraitEvent.DamageEvent.Direct) {
        if (event.attackingPlayer != null) {
            punish()
        }
    }

    override fun onTick(event: TraitEvent.TickEvent) {
        if (punishmentTime > 0) {
            punishmentTime--
        }
    }

    fun punish() {
        punishmentTime = 20 * 60
    }

    override val settings: Trait.Settings
        get() = SETTINGS

    companion object {
        val SETTINGS = Trait.Settings(
            key = TraitorGamePlugin.key("pacifist"),
            name = "Pacifist",
            traitColor = Colors.LAST_CHANCE,
            itemProvider = ItemBuilder(Material.EMERALD),
            descriptionProvider = { "Pacifist Description" },
            provider = { Pacifist() }
        )
    }
}