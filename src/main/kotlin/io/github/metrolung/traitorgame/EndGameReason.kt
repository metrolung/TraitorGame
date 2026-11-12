package io.github.metrolung.traitorgame

import io.github.metrolung.traitorgame.role.RoleAlignment
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.TextComponent
import org.bukkit.NamespacedKey

interface EndGameReason {
    val winMessage: TextComponent
    val winSound: Sound

    fun determineDefaultWinners(session: Session): Set<SessionPlayer>

    open class TraitorWin : EndGameReason {
        override val winMessage = "Traitors Win!".colored(Colors.TRAITOR_RED)

        override val winSound = Sound.sound {
            it.type(NamespacedKey.minecraft("item.goat_horn.sound.2"))
            it.source(Sound.Source.AMBIENT)
        }

        override fun determineDefaultWinners(session: Session): Set<SessionPlayer> {
            return session.allPlayers.values.filterTo(mutableSetOf()) { player -> player.role.settings.alignment.isTraitor }
        }
    }

    open class SurvivorWin : EndGameReason {
        override val winMessage = "Survivors Win!".colored(Colors.SURVIVOR_TEAL)

        override val winSound = Sound.sound {
            it.type(NamespacedKey.minecraft("item.goat_horn.sound.1"))
            it.source(Sound.Source.AMBIENT)
        }

        override fun determineDefaultWinners(session: Session): Set<SessionPlayer> {
            return session.allPlayers.values.filterTo(mutableSetOf()) { player -> player.role.settings.alignment.isSurvivor }
        }
    }

    open class DragonDefeated : SurvivorWin() {
        override val winMessage: TextComponent
            get() = "Dragon defeated!".colored(Colors.DRAGON_PURPLE)
    }

    open class NeutralWin : EndGameReason {
        override val winMessage = "Neutral Win".colored(Colors.NEUTRAL_YELLOW)

        override val winSound = Sound.sound {
            it.type(NamespacedKey.minecraft("item.goat_horn.sound.0"))
            it.source(Sound.Source.AMBIENT)
        }

        override fun determineDefaultWinners(session: Session): Set<SessionPlayer> = emptySet()
    }

    open class JesterWin(val sessionPlayer: SessionPlayer) : NeutralWin() {
        override val winSound: Sound
            get() = Sound.sound {
                it.type(NamespacedKey.minecraft("item.goat_horn.sound.7"))
                it.source(Sound.Source.AMBIENT)
            }

        override val winMessage: TextComponent
            get() = "Jester win!".colored(Colors.JESTER_PEACH)

        override fun determineDefaultWinners(session: Session): Set<SessionPlayer> {
            return super.determineDefaultWinners(session) + sessionPlayer
        }
    }

    open class Draw : NeutralWin() {
        override val winMessage = "Draw...".colored(Colors.ORANGE)
        override val winSound = Sound.sound {
            it.type(NamespacedKey.minecraft("item.goat_horn.sound.3"))
            it.source(Sound.Source.AMBIENT)
        }

        override fun determineDefaultWinners(session: Session): Set<SessionPlayer> = emptySet()
    }
}