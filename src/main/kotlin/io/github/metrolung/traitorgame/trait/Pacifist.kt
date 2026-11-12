package io.github.metrolung.traitorgame.trait

import io.github.metrolung.traitorgame.Colors
import io.github.metrolung.traitorgame.OnlineSessionPlayer
import io.github.metrolung.traitorgame.Session
import io.github.metrolung.traitorgame.TraitorGamePlugin
import io.github.metrolung.traitorgame.trait.Detective.Companion.SETTINGS
import org.bukkit.Material
import org.bukkit.entity.Item
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.item.ItemBuilder

class Pacifist : Trait {
    var punishmentTime = 0

    override fun onDealDamage(event: Trait.Event, damage: Trait.DamageInfo) {
        if (damage.attackingPlayer != null) {
            punish()
        }
    }

    override fun onTick(event: Trait.Event, tick: Int) {
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
            builder = { LastChance() }
        )
    }
}