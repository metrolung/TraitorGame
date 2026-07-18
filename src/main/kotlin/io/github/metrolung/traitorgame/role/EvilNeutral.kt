package io.github.metrolung.traitorgame.role

import io.github.metrolung.traitorgame.Colors

interface EvilNeutral : Neutral {
    override val alignment: RoleAlignment
        get() = RoleAlignment.EVIL_NEUTRAL
}