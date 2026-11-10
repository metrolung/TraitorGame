package io.github.metrolung.traitorgame.trait

import io.github.metrolung.traitorgame.EndGameReason
import io.github.metrolung.traitorgame.OnlineSessionPlayer
import io.github.metrolung.traitorgame.Session
import org.bukkit.entity.Item
import org.bukkit.inventory.ItemStack

object Victim : Trait {
    override fun onTakeDamage(event: Trait.Event, damage: Trait.DamageInfo) {
        if (damage.isKilling && damage.attackingPlayer != null) {
            event.session.forceEndSession(EndGameReason.JesterWin(event.player))
        }
    }
}