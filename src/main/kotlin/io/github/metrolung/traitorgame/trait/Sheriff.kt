package io.github.metrolung.traitorgame.trait

import io.github.metrolung.traitorgame.ItemStacks
import io.github.metrolung.traitorgame.OnlineSessionPlayer
import io.github.metrolung.traitorgame.Session
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class Sheriff : Trait {
    override fun onRolePresented(session: Session, sessionPlayer: OnlineSessionPlayer) {
        sessionPlayer.player.give(ItemStacks.sheriffCrossbow)
        sessionPlayer.player.give(ItemStack.of(Material.ARROW).apply { amount = 32 })
    }
}