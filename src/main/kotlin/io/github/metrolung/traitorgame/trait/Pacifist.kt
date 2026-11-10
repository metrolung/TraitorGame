package io.github.metrolung.traitorgame.trait

import io.github.metrolung.traitorgame.OnlineSessionPlayer
import io.github.metrolung.traitorgame.Session
import org.bukkit.entity.Item
import org.bukkit.inventory.ItemStack

class Pacifist : Trait {
    var punishmentTime = 0

    override fun onDealDamage(event: Trait.Event, damage: Trait.DamageInfo) {
        if (damage.attackingPlayer != null) {
            punish()
        }
    }

    override fun onTickOnline(session: Session, sessionPlayer: OnlineSessionPlayer, tick: Int) {
        if (punishmentTime > 0) {
            punishmentTime--
        }
    }

    fun punish() {
        punishmentTime = 20 * 60
    }
}