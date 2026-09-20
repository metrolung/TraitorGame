package io.github.metrolung.traitorgame.achievements

import net.kyori.adventure.text.Component

interface Achievement {
    val name: Component
    val description: Component?
        get() = null
    val secondsBeforeExposure: IntRange
}