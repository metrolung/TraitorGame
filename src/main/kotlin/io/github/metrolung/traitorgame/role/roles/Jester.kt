package io.github.metrolung.traitorgame.role.roles

import io.github.metrolung.traitorgame.Colors
import io.github.metrolung.traitorgame.EndGameReason
import io.github.metrolung.traitorgame.OnlineSessionPlayer
import io.github.metrolung.traitorgame.Session
import io.github.metrolung.traitorgame.SessionPlayer
import io.github.metrolung.traitorgame.colored
import io.github.metrolung.traitorgame.role.Neutral
import io.github.metrolung.traitorgame.role.RoleSettings
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.time.Duration.Companion.seconds

class Jester : Neutral {
    var punishmentRemaining: Int = 0
    var hasBeenPunished: Boolean = false

    override val roleColor: Int
        get() = Colors.JESTER_PEACH

    override val name: String = "Jester"

    override val isEvil: Boolean
        get() = true

    override fun getGoal(roleSettings: RoleSettings): String {
        return "Get killed by a non evil player. Avoid attacking players as much as possible."
    }

    override fun isWinner(session: Session, sessionPlayer: SessionPlayer, endGameReason: EndGameReason): Boolean {
        if (endGameReason is EndGameReason.JesterWin && endGameReason.sessionPlayer.uniqueId == sessionPlayer.uniqueId) {
            return true
        }

        return super.isWinner(session, sessionPlayer, endGameReason)
    }

    override fun onAttacking(session: Session, sessionPlayer: OnlineSessionPlayer, victim: OnlineSessionPlayer): Boolean {
        punishmentRemaining = session.settings.roleSettings.jesterPunishmentTicks

        if (!hasBeenPunished) {
            sessionPlayer.player.sendMessage(
                (
                    "You've been punished for attacking a player.\n" +
                    "Any deaths within the next ${(punishmentRemaining / 20.0).toInt().seconds} won't count as a win."
                ).colored(Colors.VERY_RED)
            )

            hasBeenPunished = true
        }

        return super.onAttacking(session, sessionPlayer, victim)
    }

    override fun onKilled(session: Session, sessionPlayer: OnlineSessionPlayer, killer: OnlineSessionPlayer): Boolean {
        if (punishmentRemaining <= 0 && !killer.role.isEvil) {
            session.manager.endSession(EndGameReason.JesterWin(sessionPlayer.sessionPlayer))
            sessionPlayer.player.addPotionEffect(PotionEffect(PotionEffectType.LEVITATION, 20*10, 0, false, false, false))
            return true
        }

        return super.onKilled(session, sessionPlayer, killer)
    }

    override fun onTickOnline(session: Session, sessionPlayer: OnlineSessionPlayer, tick: Int) {
        if (punishmentRemaining <= 0) {
            return
        }

        punishmentRemaining--

        sessionPlayer.player.sendActionBar(
            "Don't die for the next ${(punishmentRemaining / 20.0).toInt().seconds}".colored(Colors.VERY_YELLOW)
        )

//        if (tick % 30 == 0) {
//            for (player in session.server.onlinePlayers) {
//                if (player.gameMode == GameMode.SPECTATOR) {
//                    val offset = 3.0
//                    sessionPlayer.player.spawnParticle(
//                        Particle.SCULK_SOUL,
//                        player.location,
//                        1,
//                        offset,
//                        offset,
//                        offset,
//                        0.05
//                    )
//                }
//            }
//        }
    }
}