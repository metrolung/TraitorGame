package io.github.metrolung.traitorgame.role.roles

import io.github.metrolung.traitorgame.Colors
import io.github.metrolung.traitorgame.OnlineSessionPlayer
import io.github.metrolung.traitorgame.Session
import io.github.metrolung.traitorgame.TraitorGamePlugin
import io.github.metrolung.traitorgame.role.Role
import io.github.metrolung.traitorgame.role.RoleAlignment
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Particle
import xyz.xenondevs.invui.item.ItemBuilder

class Astral : Survivor {
    override fun onTickOnline(session: Session, sessionPlayer: OnlineSessionPlayer, tick: Int) {
        if (tick % 30 == 0) {
            for (player in session.server.onlinePlayers) {
                if (player.gameMode == GameMode.SPECTATOR) {
                    val offset = 3.0
                    sessionPlayer.player.spawnParticle(
                        Particle.SCULK_SOUL,
                        player.location,
                        1,
                        offset,
                        offset,
                        offset,
                        0.05
                    )
                }
            }
        }

        super.onTickOnline(session, sessionPlayer, tick)
    }

    override val settings: Role.Settings
        get() = SETTINGS

    companion object {
        @JvmField
        val SETTINGS: Role.Settings = Role.Settings(
            key = TraitorGamePlugin.key("astral"),
            name = "Astral",
            roleColor = Colors.ASTRAL_PURPLE,
            alignment = RoleAlignment.SURVIVOR,
            itemProvider = ItemBuilder(Material.DIAMOND),
            goalProvider = { "Work with the survivors. You can see ghosts." },
            builder = { Astral() }
        )
    }
}