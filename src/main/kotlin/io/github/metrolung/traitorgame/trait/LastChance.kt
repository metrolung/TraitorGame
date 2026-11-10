package io.github.metrolung.traitorgame.trait

import io.github.metrolung.traitorgame.ItemStacks
import io.github.metrolung.traitorgame.OnlineSessionPlayer
import io.github.metrolung.traitorgame.Session
import io.github.metrolung.traitorgame.SessionPlayer
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class LastChance : Trait {
    override fun onPlayerDied(session: Session, sessionPlayer: SessionPlayer, deadPlayer: SessionPlayer) {
        super.onPlayerDied(session, sessionPlayer, deadPlayer)
    }
}