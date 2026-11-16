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
            val groups = sortedMapOf<SampleGroup<T>?, MutableList<() -> T>>(Comparator { p0, p1 ->
                if (p0 == null && p1 == null)
                    return@Comparator 0

                if (p0 == null) {
                    return@Comparator 1
                }

                if (p1 == null) {
                    return@Comparator 0
                }

                if (p1.priority == p0.priority) {
                    return@Comparator p0.hashCode().compareTo(p1.hashCode())
                }

                p1.priority.compareTo(p0.priority)
            })

            for ((_, sampleGroup) in sampleGroups) {
                groups[sampleGroup] = mutableListOf()
            }

            groups[null] = mutableListOf()


            for (sample in samples) {
                val groupIdentifier = sampleGroups[sample.group]

                val list = groups[groupIdentifier] ?: continue

                repeat(sample.minAmount) {
                    list.add(sample.provider)
                }

                repeat(sample.maxAmount - sample.minAmount) {
                    if (random.nextDouble() < sample.chance) {
                        list.add(sample.provider)
                    }
                }
            }

            val items = mutableListOf<T>()
            outer@for ((group, groupSamples) in groups) {
                groupSamples.shuffle(random)
                group?.limit?.let { limit ->
                    if (groupSamples.size > limit) {
                        groupSamples.dropLast(groupSamples.size - limit)
                    }
                    if (groupSamples.size < limit && group.groupDefault != null) {
                        repeat(limit - groupSamples.size) {
                            groupSamples.add(group.groupDefault)
                        }
                    }
                }

                for (provider in groupSamples) {
                    if (items.size >= size) {
                        break@outer
                    }
                    items.add(provider())
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