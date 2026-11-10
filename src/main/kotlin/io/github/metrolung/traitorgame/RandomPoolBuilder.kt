package io.github.metrolung.traitorgame

import org.checkerframework.common.returnsreceiver.qual.This
import java.util.SortedMap
import kotlin.random.Random

class RandomPoolBuilder<T>(
    var size: Int,
    var default: () -> T,
    var random: Random = Random
) {
    val samples = mutableListOf<Sample<T>>()

    fun addSample(sample: Sample<T>) {
        samples.add(sample)
    }

    fun setSize(size: Int): @This RandomPoolBuilder<T> {
        this.size = size
        return this
    }

    fun setRandom(random: Random = Random): @This RandomPoolBuilder<T> {
        this.random = random
        return this
    }

    fun setDefault(default: () -> T): @This RandomPoolBuilder<T> {
        this.default = default
        return this
    }

    fun addSample(
        maxAmount: Int,
        minAmount: Int = 0,
        chance: Double = 1.0,
        priority: Double = 0.0,
        itemBuilder: () -> T
    ) {
        addSample(Sample(
            priority = priority,
            minAmount = minAmount,
            maxAmount = maxAmount,
            chance = chance,
            itemBuilder = itemBuilder
        ))
    }

    fun addItem(item: T, chance: Double, priority: Double) {
        addSample(1, chance = chance, priority = priority) { item }
    }

    fun build(): List<T> {
        return newPool(size, samples, random, default)
    }


    data class Sample<T>(
        val maxAmount: Int,
        val minAmount: Int,
        val chance: Double,
        val priority: Double,
        val itemBuilder: () -> T
    )

    companion object {
        @JvmStatic
        fun <T> newPool(size: Int, samples: List<Sample<T>>, random: Random = Random, default: () -> T): List<T> {
            val priorityGroups: SortedMap<Double, MutableList<T>> = sortedMapOf(reverseOrder())

            for (sample in samples) {
                if (!priorityGroups.containsKey(sample.priority)) {
                    priorityGroups[sample.priority] = mutableListOf()
                }
                val group: MutableList<T> = priorityGroups[sample.priority]!!

                repeat(sample.minAmount) {
                    group.add(sample.itemBuilder())
                }

                repeat(sample.maxAmount - sample.minAmount) {
                    if (random.nextDouble() > sample.chance) {
                        group.add(sample.itemBuilder())
                    }
                }

                group.shuffle()
            }

            val items = mutableListOf<T>()
            outer@ for (group in priorityGroups.values) {
                for (item in group) {
                    if (items.size >= size) {
                        break@outer
                    }
                    items.add(item)
                }
            }

            if (items.size < size) {
                repeat(size - items.size) {
                    items.add(default())
                }
            }

            items.shuffle()

            return items
        }
    }


    // Does not guarantee the result will be shuffled fairly
//    fun equalPool(
//        size: Int,
////        fixedRoles: List<Pair<Role.Settings, Int>>,
////        weightedRoles: List<Pair<Role.Settings, Double>>,
////        fallback: Role.Settings
//    ): List<Role> {
//        if (size == 0) {
//            return emptyList()
//        }
//
//        val pool = mutableListOf<Role>()
//        for ((role, amount) in fixedRoles) {
//            repeat(amount) {
//                pool.add(role.builder.build())
//            }
//        }
//
//        if (pool.size >= size) {
//            pool.shuffle()
//            return pool.subList(0, size)
//        }
//
//        val remaining = size - pool.size
//        if (weightedRoles.isEmpty()) {
//            repeat(remaining) {
//                pool.add(fallback.builder.build())
//            }
//            return pool
//        }
//
//        if (weightedRoles.size == 1) {
//            repeat(remaining) {
//                pool.add(weightedRoles.first().first.builder.build())
//            }
//            return pool
//        }
//
//        var totalWeight = weightedRoles.sumOf { (_, weight) -> weight }
//        var carry = 0.0
//        for ((role, weight) in weightedRoles.shuffled()) {
//            val mappedWeight = totalWeight / weight * remaining
//            repeat((carry + mappedWeight).roundToInt() - carry.roundToInt()) {
//                pool.add(role.builder.build())
//            }
//            carry += mappedWeight
//        }
//
//        return pool
//    }

//    // Guarantees a fair shuffled result
//    fun generatePool(
//        playerCount: Int,
//        traitorCount: Int,
//        passiveNeutralCount: Int,
//        evilNeutralCount: Int
//    ): List<Role> {
//        var remaining = playerCount
//        val actualTraitorCount = min(remaining, traitorCount)
//        remaining -= actualTraitorCount
//
//        // Just deal with all the RNG in one fell swoop so it doesn't cause problems
//        val fixedRoles = mutableListOf<Pair<Role.Settings, Int>>()
//        val weightedRoles = mutableListOf<Pair<Role.Settings, Double>>()
//
//        for (role in roles.values) {
//            when (role.rolePoolPredicate) {
//                RolePoolPredicate.Ignored -> {}
//                RolePoolPredicate.Fixed -> {
//                    val amount = (0..<role.amount).sumOf { _ -> 1 * (role.chance > Random.nextDouble()) }
//                    fixedRoles.add(Pair(role.settings, amount))
//                }
//                RolePoolPredicate.Weighted -> {
//                    val weight = if (role.minWeight < role.maxWeight) {
//                        Random.nextDouble(role.minWeight, role.maxWeight)
//                    } else {
//                        role.minWeight
//                    }
//                    weightedRoles.add(Pair(role.settings, weight))
//                }
//            }
//        }
//
//        val actualPassiveNeutralCount = min(remaining, if (weightedRoles.any { (role, _) -> role.alignment.isPassiveNeutral }) {
//            passiveNeutralCount
//        } else {
//            min(passiveNeutralCount, fixedRoles.sumOf { (role, amount) ->
//                amount * role.alignment.isPassiveNeutral
//            })
//        })
//        remaining -= actualPassiveNeutralCount
//
//        val actualEvilNeutralCount = min(remaining, if (weightedRoles.any { (role, _) -> role.alignment.isEvilNeutral }) {
//            evilNeutralCount
//        } else {
//            min(evilNeutralCount, fixedRoles.sumOf { (role, amount) ->
//                amount * role.alignment.isEvilNeutral
//            })
//        })
//        remaining -= actualEvilNeutralCount
//
//        val traitorPool = generateSubPool(
//            actualTraitorCount,
//            fixedRoles.filter { (role, _) -> role.alignment.isTraitor },
//            weightedRoles.filter { (role, _) -> role.alignment.isTraitor },
//            Traitor.SETTINGS
//        )
//        val passiveNeutralPool = generateSubPool(
//            actualPassiveNeutralCount,
//            fixedRoles.filter { (role, _) -> role.alignment.isPassiveNeutral },
//            weightedRoles.filter { (role, _) -> role.alignment.isPassiveNeutral },
//            Survivor.SETTINGS
//        )
//        val evilNeutralPool = generateSubPool(
//            actualEvilNeutralCount,
//            fixedRoles.filter { (role, _) -> role.alignment.isEvilNeutral },
//            weightedRoles.filter { (role, _) -> role.alignment.isEvilNeutral },
//            Survivor.SETTINGS
//        )
//        val survivorPool = generateSubPool(
//            remaining,
//            fixedRoles.filter { (role, _) -> role.alignment.isSurvivor },
//            weightedRoles.filter { (role, _) -> role.alignment.isSurvivor },
//            Survivor.SETTINGS
//        )
//
//        return (traitorPool + passiveNeutralPool + evilNeutralPool + survivorPool).shuffled()
//    }

}