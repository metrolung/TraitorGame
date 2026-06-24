package io.github.metrolung.traitorgame.role

import kotlin.math.min
import kotlin.random.Random

object RolePicker {
    // Returns a list of roles that is at least the size of the players
    fun newRoleSelection(playerCount: Int, roleSettings: RoleSettings): List<Role.Generator> {
        val roleList = mutableListOf<Role.Generator>()

        fun determineAvailable(rolePool: List<Role.Setting>): List<Role.Generator> {
            val availableRoles = mutableListOf<Role.Generator>()
            for (roleConfig in rolePool) {
                for (i in 0..<roleConfig.amount) {
                    if (roleConfig.chance > Random.nextDouble()) {
                        availableRoles.add(roleConfig.role)
                    }
                }
            }
            availableRoles.shuffle()

            return availableRoles
        }

        val availableTraitorRoles = determineAvailable(roleSettings.traitorRolePool)
        val availableNeutralRoles = determineAvailable(roleSettings.neutralRolePool)
        val availableSurvivorRoles = determineAvailable(roleSettings.survivorRolePool)

        for (i in 0..<roleSettings.traitorCount) {
            roleList += availableTraitorRoles.getOrNull(i) ?: roleSettings.unassignedTraitor
        }

        for (i in 0..<min(availableNeutralRoles.size, roleSettings.neutralCount)) {
            roleList += availableNeutralRoles[i]
        }

        val remaining = playerCount-roleList.size
        if (remaining > 0) {
            for (i in 0..<remaining) {
                roleList += availableSurvivorRoles.getOrNull(i) ?: roleSettings.unassignedSurvivor
            }
        }

        return roleList
    }
}