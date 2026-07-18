package io.github.metrolung.traitorgame.role

import io.github.metrolung.traitorgame.Colors
import io.github.metrolung.traitorgame.colored
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.NamespacedKey

enum class RoleAlignment {
    TRAITOR,
    EVIL_NEUTRAL,
    PASSIVE_NEUTRAL,
    SURVIVOR;

//    val winSound: Sound
//        get() = Sound.sound {
//            when (this) {
//                TRAITOR -> {
//                    it.type(NamespacedKey.minecraft("item.goat_horn.sound.2"))
//                    it.source(Sound.Source.AMBIENT)
//                }
//                EVIL_NEUTRAL -> {
//                    it.type(NamespacedKey.minecraft("item.goat_horn.sound.2"))
//                    it.source(Sound.Source.AMBIENT)
//                }
//                PASSIVE_NEUTRAL -> {
//                    it.type(NamespacedKey.minecraft("item.goat_horn.sound.0"))
//                    it.source(Sound.Source.AMBIENT)
//                }
//                SURVIVOR -> {
//                    it.type(NamespacedKey.minecraft("item.goat_horn.sound.1"))
//                    it.source(Sound.Source.AMBIENT)
//                }
//            }
//        }

    val isTraitor: Boolean
        get() = this == TRAITOR

    val isEvilNeutral: Boolean
        get() = this == EVIL_NEUTRAL

    val isPassiveNeutral: Boolean
        get() = this == PASSIVE_NEUTRAL

    val isSurvivor: Boolean
        get() = this == SURVIVOR

    val isEvil: Boolean
        get() = this.isTraitor || this.isEvilNeutral

    val isInnocent: Boolean
        get() = this.isSurvivor || this.isPassiveNeutral

    val isNeutral: Boolean
        get() = this.isPassiveNeutral || this.isEvilNeutral

//    val winMessage: TextComponent
//        get() = when (this) {
//            TRAITOR -> "Traitors Win!".colored(Colors.TRAITOR_RED)
//            EVIL_NEUTRAL -> "Neutral Win".colored(Colors.NEUTRAL_YELLOW)
//            PASSIVE_NEUTRAL -> "Neutral Win".colored(Colors.NEUTRAL_YELLOW)
//            SURVIVOR -> "Survivors Win!".colored(Colors.SURVIVOR_TEAL)
//        }

    val stylized: TextComponent
        get() = when (this) {
            TRAITOR -> "Traitor".colored(Colors.TRAITOR_RED)
            EVIL_NEUTRAL -> "Evil ".colored(Colors.TRAITOR_RED).append("Neutral".colored(Colors.NEUTRAL_YELLOW))
                .hoverEvent(HoverEvent.showText("You can only win through selfish means".colored(Colors.WHITE)))
            PASSIVE_NEUTRAL -> "Passive ".colored(Colors.SURVIVOR_TEAL).append("Neutral".colored(Colors.NEUTRAL_YELLOW))
                .hoverEvent(HoverEvent.showText("You win alongside another team by completing your objective".colored(Colors.WHITE)))
            SURVIVOR -> "Survivor".colored(Colors.SURVIVOR_TEAL)
        }
}

//sealed interface RoleType {
//    val name: String
//    val collectiveName: String
//    val color: Int
//    val winSound: Sound
//
//    val winMessage: TextComponent
//        get() = "$collectiveName win!".colored(color)
//    val stylized: TextComponent
//        get() = name.colored(color)
//
//    val isTraitor: Boolean
//        get() = this is RoleType.Traitor
//    val isNeutral: Boolean
//        get() = this is RoleType.Neutral
//    val isSurvivor: Boolean
//        get() = this is RoleType.Survivor
//
//    object Traitor : RoleType {
//        override val name = "Traitor"
//        override val collectiveName = "Traitors"
//
//        override val color = Colors.TRAITOR_RED
//
//        override val winSound = Sound.sound {
//            it.type(NamespacedKey.minecraft("item.goat_horn.sound.2"))
//            it.source(Sound.Source.AMBIENT)
//        }
//    }
//    object Neutral : RoleType {
//        override val name = "Neutral"
//        override val collectiveName = "Neutral"
//
//        override val color = Colors.NEUTRAL_YELLOW
//
//        override val winSound: Sound = Sound.sound {
//            it.type(NamespacedKey.minecraft("item.goat_horn.sound.0"))
//            it.source(Sound.Source.AMBIENT)
//        }
//    }
//    object Survivor : RoleType {
//        override val name = "Survivor"
//        override val collectiveName = "Survivors"
//
//        override val color = Colors.SURVIVOR_TEAL
//
//        override val winSound = Sound.sound {
//                it.type(NamespacedKey.minecraft("item.goat_horn.sound.1"))
//                it.source(Sound.Source.AMBIENT)
//            }
//    }
//}