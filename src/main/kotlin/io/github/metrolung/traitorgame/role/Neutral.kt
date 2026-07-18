package io.github.metrolung.traitorgame.role

import io.github.metrolung.traitorgame.Colors

interface Neutral : Role {
    override val roleColor: Int
        get() = Colors.NEUTRAL_YELLOW

}