package io.github.metrolung.traitorgame.role

import io.github.metrolung.traitorgame.Colors

interface PassiveNeutral : Neutral {
    override val alignment: RoleAlignment
        get() = RoleAlignment.PASSIVE_NEUTRAL
}