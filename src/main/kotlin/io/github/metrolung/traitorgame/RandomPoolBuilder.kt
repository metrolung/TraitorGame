package io.github.metrolung.traitorgame

import io.github.metrolung.traitorgame.role.Role
import io.github.metrolung.traitorgame.role.RoleAlignment
import io.github.metrolung.traitorgame.role.roles.Survivor
import io.github.metrolung.traitorgame.role.roles.Traitor
import io.github.metrolung.traitorgame.trait.Trait
import org.checkerframework.common.returnsreceiver.qual.This
import kotlin.random.Random

class RandomPoolBuilder<T>(
    var size: Int,
    var default: () -> T,
    var random: Random = Random
) {
    private val samples = mutableListOf<Sample<T>>()
    private val sampleGroups = mutableMapOf<String, SampleGroup<T>>()

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
        group: String? = null,
        provider: () -> T
    ) {
        samples.add(Sample(
            minAmount = minAmount,
            maxAmount = maxAmount,
            chance = chance,
            group = group,
            provider = provider
        ))
    }

    fun addSampleGroup(
        id: String,
        priority: Double = 0.0
    ) {
        sampleGroups[id] = SampleGroup<T>(
            id = id,
            priority = priority,
            limit = null,
            groupDefault = null
        )
    }

    fun addSampleGroup(
        id: String,
        priority: Double = 0.0,
        limit: Int? = null,
        groupDefault: (() -> T)? = null
    ) {
        sampleGroups[id] = SampleGroup(
            id = id,
            priority = priority,
            limit = limit,
            groupDefault = groupDefault
        )
    }

    fun build(): List<T> {
        return newPool(size, samples, sampleGroups, random, default)
    }

    private data class SampleGroup<T>(
        val id: String,
        val priority: Double,
        val limit: Int?,
        val groupDefault: (() -> T)?
    )

    private data class Sample<T>(
        val maxAmount: Int,
        val minAmount: Int,
        val chance: Double,
        val group: String?,
        val provider: () -> T
    )

    companion object {
        @JvmStatic
        private fun <T> newPool(size: Int, samples: List<Sample<T>>, sampleGroups: Map<String, SampleGroup<T>>, random: Random = Random, default: () -> T): List<T> {
            // Construct sample group that is associated with its contained samples
            val associatedSampleGroup = sortedMapOf<SampleGroup<T>?, MutableList<() -> T>>(Comparator { p0, p1 ->
                val priority0 = p0?.priority ?: Double.NEGATIVE_INFINITY
                val priority1 = p1?.priority ?: Double.NEGATIVE_INFINITY

                if (priority0 == priority1) {
                    return@Comparator p0.hashCode().compareTo(p1.hashCode())
                }

                priority1.compareTo(priority0)
            })

            // Add sample groups
            associatedSampleGroup[null] = mutableListOf()
            for (sampleGroup in sampleGroups.values) {
                associatedSampleGroup[sampleGroup] = mutableListOf()
            }

            // Add samples to each sample group association
            for ((maxAmount, minAmount, chance, group, provider) in samples) {
                val sampleGroup = sampleGroups[group]
                val associatedSamples = associatedSampleGroup[sampleGroup] ?: associatedSampleGroup[null]!!

                repeat(minAmount) {
                    associatedSamples.add(provider)
                }

                repeat(maxAmount - minAmount) {
                    if (random.nextDouble() < chance) {
                        associatedSamples.add(provider)
                    }
                }
            }

            // Apply limits to each group
            for ((sampleGroup, associatedSamples) in associatedSampleGroup) {
                if (sampleGroup?.limit == null) {
                    continue
                }

                val limit: Int = sampleGroup.limit

                if (associatedSamples.size > limit) {
                    // Drop extra samples if there are too many in a group
                    associatedSamples.shuffle(random)
                    repeat(associatedSamples.size - limit) {
                        associatedSamples.removeLast() // remove excess in a random order
                    }
                } else if ((associatedSamples.size < limit) && (sampleGroup.groupDefault != null)) {
                    // Add defaults if group supports them
                    repeat(limit - associatedSamples.size) {
                        associatedSamples.add(sampleGroup.groupDefault)
                    }
                }
            }

            // Construct all samples and add the items to resulting pool, breaking early if the resulting pool is the required size
            val resultingPool = mutableListOf<T>()

            for (associatedSamples in associatedSampleGroup.values) {
                for (sample in associatedSamples) {
                    if (resultingPool.size >= size) {
                        break
                    }

                    resultingPool.add(sample())
                }
            }

            // Add extra default items in case it's short of the required size
            if (resultingPool.size < size) {
                repeat(size - resultingPool.size) {
                    resultingPool.add(default())
                }
            }

            // Randomize the resulting pool
            resultingPool.shuffle(random)

            return resultingPool
        }
    }
}

private fun getRange(string: String?): IntRange? {
    if (string == null) {
        return null
    }

    val split = string.split("..")
    if (split.size == 1) {
        val max = split[0].toIntOrNull() ?: return null
        return 0..max
    }

    if (split.size == 2) {
        val min = split[0].toIntOrNull() ?: return null
        val max = split[1].toIntOrNull() ?: return null
        return min..max
    }

    return null
}

fun generateRolePool(playerCount: Int, traitorCount: Int, evilNeutralCount: Int, passiveNeutralCount: Int): List<Role> {
    val rolePool = RandomPoolBuilder<Role>(
        size = playerCount,
        default = { Survivor() },
    )

    rolePool.addSampleGroup(
        "traitor",
        priority = 3.0,
        limit = traitorCount,
        groupDefault = { Traitor() }
    )

    rolePool.addSampleGroup(
        "evilNeutral",
        priority = 1.0,
        limit = evilNeutralCount
    )

    rolePool.addSampleGroup(
        "passiveNeutral",
        priority = 2.0,
        limit = passiveNeutralCount
    )

    rolePool.addSampleGroup(
        "survivor",
        priority = 0.0
    )

    for ((roleSettings, configurationSection) in RoleRegistry.registered.values) {
        configurationSection.addDefault("chance", 100.0)
        configurationSection.addDefault("amount", "0")

        val chance = (configurationSection.getDouble("chance")) / 100.0
        val range = getRange(configurationSection.getString("amount")) ?: 0..0

        val group = when (roleSettings.alignment) {
            RoleAlignment.TRAITOR -> "traitor"
            RoleAlignment.EVIL_NEUTRAL -> "evilNeutral"
            RoleAlignment.PASSIVE_NEUTRAL -> "passiveNeutral"
            RoleAlignment.SURVIVOR -> "survivor"
        }

        rolePool.addSample(
            maxAmount = range.last,
            minAmount = range.first,
            chance = chance,
            group = group,
            provider = { roleSettings.provider() }
        )
    }

    return rolePool.build()
}

fun generateSurvivorTraitPool(playerCount: Int): List<Trait?> {
    val traitPool = RandomPoolBuilder<Trait?>(
        size = playerCount,
        default = { null }
    )

    for ((traitSettings, configurationSection) in TraitRegistry.registered.values) {
        configurationSection.addDefault("chance", 100.0)
        configurationSection.addDefault("amount", "0")

        val chance = (configurationSection.getDouble("chance")) / 100.0
        val range = getRange(configurationSection.getString("amount")) ?: 0..0

        traitPool.addSample(
            maxAmount = range.last,
            minAmount = range.first,
            chance = chance,
            provider = { traitSettings.provider() }
        )
    }

    return traitPool.build()
}