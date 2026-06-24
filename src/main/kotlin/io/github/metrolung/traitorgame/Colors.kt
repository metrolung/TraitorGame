package io.github.metrolung.traitorgame

import net.kyori.adventure.text.format.TextColor
import org.bukkit.Color


val Int.textColor get() = TextColor.color(this)
fun Int.color(alpha: Int = 255) = Color.fromARGB(this).setAlpha(alpha)


object Colors {

    const val LIGHT_GRAY = 0x888888
    const val MID_GRAY = 0x777777
    const val DARK_GRAY = 0x555555


    const val TRAITOR_RED = 0xEA0941
    const val DETECTIVE_BLUE = 0x1E41ED
    const val SURVIVOR_TEAL = 0x01FFCC
    const val NEUTRAL_YELLOW = 0xF9D41B
    const val JESTER_PEACH = 0xFC7550
    const val ASTRAL_PURPLE = 0xA94BFC



    const val DRAGON_PURPLE = 0xBD58FC

    const val MIDNIGHT_CYAN = 0x001A1A
    const val MIDNIGHT_GREEN = 0x001A00
    const val ALMOST_BLACK = 0x1A1A1A

    const val VERY_RED = 0xFF0000
    const val VERY_GREEN = 0x00FF00
    const val VERY_YELLOW = 0xFFFF00
    const val WHITE = 0xFFFFFF

    const val MEETING_GREEN = 0xAAFFAA

    const val ORANGE = 0xFC9835
    const val LAVENDER = 0xB985FC


}