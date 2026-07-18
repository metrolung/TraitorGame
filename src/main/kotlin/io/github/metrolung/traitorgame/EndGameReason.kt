package io.github.metrolung.traitorgame

import io.github.metrolung.traitorgame.role.RoleAlignment
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.TextComponent
import org.bukkit.NamespacedKey

interface EndGameReason {
    val winMessage: TextComponent
//        get() = winningAlignment?.winMessage ?: "Draw...".colored(Colors.ORANGE)

    val winSound: Sound
//        get() = winningAlignment?.winSound ?: Sound.sound {
//            it.type(NamespacedKey.minecraft("item.goat_horn.sound.3"))
//            it.source(Sound.Source.AMBIENT)
//        }

//    val winningAlignment: RoleAlignment?

//    data class AlignmentWin(override val winningAlignment: RoleAlignment) : EndGameReason {
//        override val winMessage: TextComponent
//            get() = winningAlignment.winMessage
//    }

    /*
     val winSound: Sound
        get() = Sound.sound {
            when (this) {
                TRAITOR -> {
                    it.type(NamespacedKey.minecraft("item.goat_horn.sound.2"))
                    it.source(Sound.Source.AMBIENT)
                }
                EVIL_NEUTRAL -> {
                    it.type(NamespacedKey.minecraft("item.goat_horn.sound.2"))
                    it.source(Sound.Source.AMBIENT)
                }
                PASSIVE_NEUTRAL -> {
                    it.type(NamespacedKey.minecraft("item.goat_horn.sound.0"))
                    it.source(Sound.Source.AMBIENT)
                }
                SURVIVOR -> {
                    it.type(NamespacedKey.minecraft("item.goat_horn.sound.1"))
                    it.source(Sound.Source.AMBIENT)
                }
            }
        }

        val winMessage: TextComponent
        get() = when (this) {
            TRAITOR -> "Traitors Win!".colored(Colors.TRAITOR_RED)
            EVIL_NEUTRAL -> "Neutral Win".colored(Colors.NEUTRAL_YELLOW)
            PASSIVE_NEUTRAL -> "Neutral Win".colored(Colors.NEUTRAL_YELLOW)
            SURVIVOR -> "Survivors Win!".colored(Colors.SURVIVOR_TEAL)
        }
     */


    object TraitorWin : EndGameReason {
        override val winMessage = "Traitors Win!".colored(Colors.TRAITOR_RED)

        override val winSound = Sound.sound {
            it.type(NamespacedKey.minecraft("item.goat_horn.sound.2"))
            it.source(Sound.Source.AMBIENT)
        }
    }

    object SurvivorWin : EndGameReason {
        override val winMessage = "Survivors Win!".colored(Colors.SURVIVOR_TEAL)

        override val winSound = Sound.sound {
            it.type(NamespacedKey.minecraft("item.goat_horn.sound.1"))
            it.source(Sound.Source.AMBIENT)
        }
    }

    object NeutralWin : EndGameReason {
        override val winMessage = "Neutral Win".colored(Colors.NEUTRAL_YELLOW)

        override val winSound = Sound.sound {
            it.type(NamespacedKey.minecraft("item.goat_horn.sound.0"))
            it.source(Sound.Source.AMBIENT)
        }
    }

    object DragonDefeated : EndGameReason {
        override val winMessage: TextComponent
            get() = "Dragon defeated!".colored(Colors.DRAGON_PURPLE)

        override val winSound: Sound
            get() = SurvivorWin.winSound
    }

    data class JesterWin(val sessionPlayer: SessionPlayer) : EndGameReason {
        override val winSound: Sound
            get() = Sound.sound {
                it.type(NamespacedKey.minecraft("item.goat_horn.sound.7"))
                it.source(Sound.Source.AMBIENT)
            }

        override val winMessage: TextComponent
            get() = "Jester win!".colored(Colors.JESTER_PEACH)
    }

    object Draw : EndGameReason {
        override val winMessage = "Draw...".colored(Colors.ORANGE)
        override val winSound = Sound.sound {
            it.type(NamespacedKey.minecraft("item.goat_horn.sound.3"))
            it.source(Sound.Source.AMBIENT)
        }
    }
}