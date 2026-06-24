package io.github.metrolung.traitorgame.role.roles

import io.github.metrolung.traitorgame.Colors
import io.github.metrolung.traitorgame.OnlineSessionPlayer
import io.github.metrolung.traitorgame.Session
import io.github.metrolung.traitorgame.role.Role
import io.github.metrolung.traitorgame.role.RoleSettings
import io.github.metrolung.traitorgame.role.RoleType
import io.github.metrolung.traitorgame.role.Survivor
import org.bukkit.GameMode
import org.bukkit.Particle
import kotlin.random.Random

object Astral : Survivor {
    override val roleColor: Int = Colors.ASTRAL_PURPLE
    override val name: String = "Astral"

    override fun getGoal(roleSettings: RoleSettings): String {
        return "Work with the survivors. You can see ghosts."
    }

    override fun onTickOnline(session: Session, sessionPlayer: OnlineSessionPlayer, tick: Int) {
        if (tick % 30 != 0) {
            return
        }

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
}