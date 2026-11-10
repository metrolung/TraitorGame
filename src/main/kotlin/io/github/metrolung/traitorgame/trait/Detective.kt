package io.github.metrolung.traitorgame.trait

import io.github.metrolung.traitorgame.ItemStacks
import io.github.metrolung.traitorgame.OnlineSessionPlayer
import io.github.metrolung.traitorgame.Session
import io.github.metrolung.traitorgame.SessionPlayer
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class Detective : Trait {
    override fun onRolePresented(session: Session, sessionPlayer: OnlineSessionPlayer) {
        sessionPlayer.player.give(ItemStacks.sampleChecker)
    }

    sealed interface DNASample {
        data class IdentifiableSample(val player: SessionPlayer) : DNASample
        data class IdentifiableCorpseSample(val player: SessionPlayer) : DNASample
        data class CorpseSample(val corpse: SessionPlayer, val potentials: List<SessionPlayer>) : DNASample
    }
}