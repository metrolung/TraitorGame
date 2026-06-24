package io.github.metrolung.traitorgame.role.roles

import io.github.metrolung.traitorgame.Colors
import io.github.metrolung.traitorgame.role.Role
import io.github.metrolung.traitorgame.role.RoleSettings
import io.github.metrolung.traitorgame.role.Survivor


object DefaultSurvivor : Survivor {
    override val roleColor: Int
        get() = Colors.SURVIVOR_TEAL

    override val name: String
        get() = "Survivor"

    override fun getGoal(roleSettings: RoleSettings): String {
        return "Defeat the enderdragon and don't die!"
    }
}