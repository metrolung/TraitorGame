package io.github.metrolung.traitorgame

import io.github.metrolung.traitorgame.role.RoleAlignment
import io.github.metrolung.traitorgame.role.roles.Survivor
import io.github.metrolung.traitorgame.role.roles.Traitor
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
}

fun generateRolePool(traitorCount: Int, evilNeutralCount: Int, passiveNeutralCount: Int, survivorCount: Int) {
    val traitorPool = RandomPoolBuilder(
        traitorCount,
        default = { Traitor() }
    )

    val evilNeutralPool = RandomPoolBuilder(
        evilNeutralCount,
        default = { Survivor() }
    )

    val passiveNeutralPool = RandomPoolBuilder(
        passiveNeutralCount,
        default = { Survivor() }
    )

    val survivorPool = RandomPoolBuilder(
        survivorCount,
        default = { Survivor() }
    )
//
//    for ((key, record) in RoleRegistry.registered) {
//        when (record.value.alignment) {
//            RoleAlignment.TRAITOR ->
//            RoleAlignment.EVIL_NEUTRAL -> TODO()
//            RoleAlignment.PASSIVE_NEUTRAL -> TODO()
//            RoleAlignment.SURVIVOR -> TODO()
//        }
//    }
}