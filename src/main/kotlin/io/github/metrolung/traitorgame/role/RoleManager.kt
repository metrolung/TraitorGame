package io.github.metrolung.traitorgame.role

import io.github.metrolung.traitorgame.component
import io.github.metrolung.traitorgame.role.roles.Survivor
import io.github.metrolung.traitorgame.role.roles.Traitor
import io.github.metrolung.traitorgame.times
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.checkerframework.common.returnsreceiver.qual.This
//import xyz.axiumyu.paperDialogDsl.dialog.DialogSetup
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

class RoleManager {
    class RoleConfiguration private constructor(
        val settings: Role.Settings,

        private var allowPredicateTypeChange: Boolean = true,
        rolePoolPredicate: RolePoolPredicate,

        // Applicable to `Fixed` predicate
        amount: Int = 1,
        chance: Double = 1.0,

        // Applicable to `Weighted` predicate
        minWeight: Double = 1.0,
        maxWeight: Double = 1.0,
    ) {
        var rolePoolPredicate = rolePoolPredicate
            private set
        var amount = amount
            private set
        var chance = chance
            private set
        var maxWeight = maxWeight
            private set
        var minWeight = minWeight
            private set

        fun lock(): @This RoleConfiguration {
            allowPredicateTypeChange = false
            return this
        }

        fun setWeighted(minWeight: Double, maxWeight: Double): Boolean {
            if (rolePoolPredicate != RolePoolPredicate.Weighted && !allowPredicateTypeChange) return false
            rolePoolPredicate = RolePoolPredicate.Weighted
            this.minWeight = minWeight
            this.maxWeight = maxWeight
            return true
        }

        fun setFixed(amount: Int, chance: Double): Boolean {
            if (rolePoolPredicate != RolePoolPredicate.Fixed && !allowPredicateTypeChange) return false
            rolePoolPredicate = RolePoolPredicate.Fixed
            this.amount = amount
            this.chance = chance
            return true
        }

        fun setIgnored(): Boolean {
            if (rolePoolPredicate != RolePoolPredicate.Ignored && !allowPredicateTypeChange) return false
            rolePoolPredicate = RolePoolPredicate.Ignored
            return true
        }

        companion object {
            fun ignored(
                settings: Role.Settings
            ): RoleConfiguration = RoleConfiguration(
                settings,
                rolePoolPredicate = RolePoolPredicate.Ignored,
            )

            fun weighted(
                settings: Role.Settings,
                minWeight: Double,
                maxWeight: Double,
            ) = RoleConfiguration(
                settings,
                rolePoolPredicate = RolePoolPredicate.Weighted,
                minWeight = minWeight,
                maxWeight = maxWeight,
            )

            fun fixed(
                settings: Role.Settings,
                amount: Int,
                chance: Double,
            ) = RoleConfiguration(
                settings,
                rolePoolPredicate = RolePoolPredicate.Fixed,
                amount = amount,
                chance = chance,
            )
        }
    }

    enum class RolePoolPredicate {
        Ignored,
        Fixed,
        Weighted
    }

    private val roles: MutableMap<NamespacedKey, RoleConfiguration> = mutableMapOf(
        Survivor.SETTINGS.key to RoleConfiguration.weighted(
            settings = Survivor.SETTINGS,
            minWeight = 0.0,
            maxWeight = 0.0,
        ).lock(),
        Traitor.SETTINGS.key to RoleConfiguration.weighted(
            settings = Traitor.SETTINGS,
            minWeight = 0.0,
            maxWeight = 0.0,
        ).lock()
    )

    fun registerRole(
        roleSettings: Role.Settings
    ): RoleConfiguration {
        val configuration = RoleConfiguration.ignored(roleSettings)
        roles[roleSettings.key] = configuration
        return configuration
    }

    fun getRoleConfiguration(key: NamespacedKey): RoleConfiguration? = roles[key]
    fun getRoleConfiguration(roleSettings: Role.Settings): RoleConfiguration? = roles[roleSettings.key]

    val keys get() = roles.keys

    // Does not guarantee the result will be shuffled fairly
    private fun generateSubPool(
        size: Int,
        fixedRoles: List<Pair<Role.Settings, Int>>,
        weightedRoles: List<Pair<Role.Settings, Double>>,
        fallback: Role.Settings
    ): List<Role> {
        if (size == 0) {
            return emptyList()
        }

        val pool = mutableListOf<Role>()
        for ((role, amount) in fixedRoles) {
            repeat(amount) {
                pool.add(role.builder.build())
            }
        }

        if (pool.size >= size) {
            pool.shuffle()
            return pool.subList(0, size)
        }

        val remaining = size - pool.size
        if (weightedRoles.isEmpty()) {
            repeat(remaining) {
                pool.add(fallback.builder.build())
            }
            return pool
        }

        if (weightedRoles.size == 1) {
            repeat(remaining) {
                pool.add(weightedRoles.first().first.builder.build())
            }
            return pool
        }

        var totalWeight = weightedRoles.sumOf { (_, weight) -> weight }
        var carry = 0.0
        for ((role, weight) in weightedRoles.shuffled()) {
            val mappedWeight = totalWeight / weight * remaining
            repeat((carry + mappedWeight).roundToInt() - carry.roundToInt()) {
                pool.add(role.builder.build())
            }
            carry += mappedWeight
        }

        return pool
    }

    // Guarantees a fair shuffled result
    fun generatePool(
        playerCount: Int,
        traitorCount: Int,
        passiveNeutralCount: Int,
        evilNeutralCount: Int
    ): List<Role> {
        var remaining = playerCount
        val actualTraitorCount = min(remaining, traitorCount)
        remaining -= actualTraitorCount

        // Just deal with all the RNG in one fell swoop so it doesn't cause problems
        val fixedRoles = mutableListOf<Pair<Role.Settings, Int>>()
        val weightedRoles = mutableListOf<Pair<Role.Settings, Double>>()

        for (role in roles.values) {
            when (role.rolePoolPredicate) {
                RolePoolPredicate.Ignored -> {}
                RolePoolPredicate.Fixed -> {
                    val amount = (0..<role.amount).sumOf { _ -> 1 * (role.chance > Random.nextDouble()) }
                    fixedRoles.add(Pair(role.settings, amount))
                }
                RolePoolPredicate.Weighted -> {
                    val weight = if (role.minWeight < role.maxWeight) {
                        Random.nextDouble(role.minWeight, role.maxWeight)
                    } else {
                        role.minWeight
                    }
                    weightedRoles.add(Pair(role.settings, weight))
                }
            }
        }

        val actualPassiveNeutralCount = min(remaining, if (weightedRoles.any { (role, _) -> role.alignment.isPassiveNeutral }) {
            passiveNeutralCount
        } else {
            min(passiveNeutralCount, fixedRoles.sumOf { (role, amount) ->
                amount * role.alignment.isPassiveNeutral
            })
        })
        remaining -= actualPassiveNeutralCount

        val actualEvilNeutralCount = min(remaining, if (weightedRoles.any { (role, _) -> role.alignment.isEvilNeutral }) {
            evilNeutralCount
        } else {
            min(evilNeutralCount, fixedRoles.sumOf { (role, amount) ->
                amount * role.alignment.isEvilNeutral
            })
        })
        remaining -= actualEvilNeutralCount

        val traitorPool = generateSubPool(
            actualTraitorCount,
            fixedRoles.filter { (role, _) -> role.alignment.isTraitor },
            weightedRoles.filter { (role, _) -> role.alignment.isTraitor },
            Traitor.SETTINGS
        )
        val passiveNeutralPool = generateSubPool(
            actualPassiveNeutralCount,
            fixedRoles.filter { (role, _) -> role.alignment.isPassiveNeutral },
            weightedRoles.filter { (role, _) -> role.alignment.isPassiveNeutral },
            Survivor.SETTINGS
        )
        val evilNeutralPool = generateSubPool(
            actualEvilNeutralCount,
            fixedRoles.filter { (role, _) -> role.alignment.isEvilNeutral },
            weightedRoles.filter { (role, _) -> role.alignment.isEvilNeutral },
            Survivor.SETTINGS
        )
        val survivorPool = generateSubPool(
            remaining,
            fixedRoles.filter { (role, _) -> role.alignment.isSurvivor },
            weightedRoles.filter { (role, _) -> role.alignment.isSurvivor },
            Survivor.SETTINGS
        )

        return (traitorPool + passiveNeutralPool + evilNeutralPool + survivorPool).shuffled()
    }

//    fun openMenu(player: Player) {
//        DialogSetup {
//            DialogContent("Hello".component)
//        }
//    }
}