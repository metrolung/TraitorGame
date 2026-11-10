package io.github.metrolung.traitorgame

import io.papermc.paper.registry.TypedKey
import net.kyori.adventure.key.Key
import kotlin.reflect.KClass



class ClassKey<T>(
    val cls: Class<T>,
    val key: Key,
)

class TypedDataHolder {

    val backing = mutableMapOf<Key, Any?>()

    operator fun <T> set(classKey: ClassKey<T>, value: T) {
        backing[classKey.key] = value
    }

    operator fun <T> get(classKey: ClassKey<T>): T? {
        val value = backing[classKey.key]
        return classKey.cls.cast(value)
    }

}