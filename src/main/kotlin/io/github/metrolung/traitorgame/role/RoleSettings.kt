package io.github.metrolung.traitorgame.role

import io.github.metrolung.traitorgame.role.roles.Detective
import io.github.metrolung.traitorgame.role.roles.Mogul
import io.github.metrolung.traitorgame.role.roles.DefaultSurvivor
import io.github.metrolung.traitorgame.role.roles.DefaultTraitor

data class RoleSettings(
    val traitorCount: Int,
    val neutralCount: Int,

    val traitorRolePool: List<RoleConfig>,
    val neutralRolePool: List<RoleConfig>,
    val survivorRolePool: List<RoleConfig>,

    val unassignedTraitor: Role.Generator,
    val unassignedSurvivor: Role.Generator,

    val traitorCodeWord: String,
    val blackMarketeerMoneyGoal: Long
) {
    companion object {
        fun create(traitorCount: Int, detectiveCount: Int, neutralCount: Int): RoleSettings {
            return RoleSettings(
                traitorCount,
                neutralCount,
                traitorRolePool = listOf(),
                neutralRolePool = listOf(
                    RoleConfig({ Mogul() }, neutralCount, 1.0)
                ),
                survivorRolePool = listOf(
                    RoleConfig({ Detective() }, detectiveCount, 1.0)
                ),
                unassignedTraitor = { DefaultTraitor },
                unassignedSurvivor = { DefaultSurvivor },
                traitorCodeWord = Traitor.codeWords.random(),
                blackMarketeerMoneyGoal = 1_000_00
            )
        }

    }
}
data class RoleConfig(val role: Role.Generator, val amount: Int, val chance: Double)