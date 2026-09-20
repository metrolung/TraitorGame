package io.github.metrolung.traitorgame.role.roles

import io.github.metrolung.traitorgame.Colors
import io.github.metrolung.traitorgame.EndGameReason
import io.github.metrolung.traitorgame.OnlineSessionPlayer
import io.github.metrolung.traitorgame.Session
import io.github.metrolung.traitorgame.SessionPlayer
import io.github.metrolung.traitorgame.TraitorGamePlugin
import io.github.metrolung.traitorgame.colored
import io.github.metrolung.traitorgame.component
import io.github.metrolung.traitorgame.role.Neutral
import io.github.metrolung.traitorgame.role.Role
import io.github.metrolung.traitorgame.role.RoleAlignment
import org.bukkit.Material
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import xyz.xenondevs.invui.item.ItemBuilder
import kotlin.time.Duration.Companion.seconds

class Jester : Neutral {
    var punishmentRemaining: Int = 0
    var hasBeenPunished: Boolean = false


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
        if (punishmentRemaining <= 0 && !killer.role.settings.alignment.isEvil) {
            session.manager.endSession(EndGameReason.JesterWin(sessionPlayer.sessionPlayer))
            sessionPlayer.player.addPotionEffect(PotionEffect(PotionEffectType.LEVITATION, 20*10, 0, false, false, false))
            return true
        }

        return super.onKilled(session, sessionPlayer, killer)
    }

    override fun onTickOnline(session: Session, sessionPlayer: OnlineSessionPlayer, tick: Int) {
        if (punishmentRemaining <= 0) {
            sessionPlayer.statusBar.hideMessage(TraitorGamePlugin.key("jester.punishment"))
            return
        }

        punishmentRemaining--

        sessionPlayer.statusBar.sendMessage(
            TraitorGamePlugin.key("jester.punishment"),
            "Don't die for the next ${(punishmentRemaining / 20.0).toInt().seconds}".colored(Colors.VERY_YELLOW),
            important = true
        )
    }

    override val settings: Role.Settings
        get() = SETTINGS

    companion object {
        @JvmField
        val SETTINGS: Role.Settings = Role.Settings(
            key = TraitorGamePlugin.key("jester"),
            name = "Jester",
            roleColor = Colors.JESTER_PEACH,
            alignment = RoleAlignment.EVIL_NEUTRAL,
            itemProvider = ItemBuilder(Material.CROSSBOW),
            goalProvider = { "Get killed by a non evil player. Avoid attacking players as much as possible." },
            builder = { Jester() }
        )
    }
}