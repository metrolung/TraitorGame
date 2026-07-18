package io.github.metrolung.traitorgame.role.roles

import io.github.metrolung.traitorgame.Colors
import io.github.metrolung.traitorgame.OnlineSessionPlayer
import io.github.metrolung.traitorgame.Session
import io.github.metrolung.traitorgame.role.RoleSettings
import org.bukkit.GameMode
import org.bukkit.Particle

class Astral : Survivor {
    override val roleColor: Int = Colors.ASTRAL_PURPLE
    override val name: String = "Astral"

    override fun getGoal(roleSettings: RoleSettings): String {
        return "Work with the survivors. You can see ghosts."
    }

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
}