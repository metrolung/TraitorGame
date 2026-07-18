package io.github.metrolung.traitorgame.role

import kotlin.math.min
import kotlin.random.Random

object RolePicker {
    // Returns a list of roles that is at least the size of the players
    fun newRoleSelection(playerCount: Int, roleSettings: RoleSettings): List<Role.Builder> {
        val roleList = mutableListOf<Role.Builder>()

        fun determineAvailable(rolePool: List<Role.Setting>): List<Role.Builder> {
            val availableRoles = mutableListOf<Role.Builder>()
            for (roleConfig in rolePool) {
                for (i in 0..<roleConfig.amount) {
                    if (roleConfig.chance > Random.nextDouble()) {
                        availableRoles.add(roleConfig.builder)
                    }
                }
            }
            availableRoles.shuffle()

            return availableRoles
        }

        val availableTraitorRoles = determineAvailable(roleSettings.traitorRolePool)
        val availablePassiveNeutralRoles = determineAvailable(roleSettings.passiveNeutralRolePool)
        val availableEvilNeutralRoles = determineAvailable(roleSettings.evilNeutralRolePool)
        val availableSurvivorRoles = determineAvailable(roleSettings.survivorRolePool)

        for (i in 0..<roleSettings.traitorCount) {
            roleList += availableTraitorRoles.getOrNull(i) ?: roleSettings.unassignedTraitor
        }

        for (i in 0..<min(availableEvilNeutralRoles.size, roleSettings.evilNeutralCount)) {
            roleList += availableEvilNeutralRoles[i]
        }

        for (i in 0..<min(availablePassiveNeutralRoles.size, roleSettings.passiveNeutralCount)) {
            roleList += availablePassiveNeutralRoles[i]
        }

        val remaining = playerCount-roleList.size
        if (remaining > 0) {
            for (i in 0..<remaining) {
                roleList += availableSurvivorRoles.getOrNull(i) ?: roleSettings.unassignedSurvivor
            }
        }

        roleList.shuffle()

        return roleList
    }
}