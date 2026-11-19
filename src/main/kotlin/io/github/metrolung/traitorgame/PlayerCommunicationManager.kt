package io.github.metrolung.traitorgame

import io.papermc.paper.math.Position
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity

class ActionBar(val divider: Component = " | ".colored(Colors.LIGHT_GRAY)) {
    private var ticker = 0
    private val temporaryMessages: MutableList<Triple<Int, TextComponent, Boolean>> = mutableListOf()
    private val messages: MutableMap<NamespacedKey, Pair<TextComponent, Boolean>> = mutableMapOf()

    fun getMessage(): TextComponent {
        val important = temporaryMessages.mapNotNull { (duration, text, important) ->
            if (important) text else null
        } + messages.toList().mapNotNull { (key, data) ->
            if (data.second) data.first else null
        }

        val notImportant = temporaryMessages.mapNotNull { (duration, text, important) ->
            if (!important) text else null
        } + messages.toList().mapNotNull { (key, data) ->
            if (!data.second) data.first else null
        }

        val allMessages = buildList {
            addAll(notImportant.subList(0, notImportant.size/2))
            addAll(important)
            addAll(notImportant.subList(notImportant.size/2, notImportant.size))
        }

        if (allMessages.isEmpty()) {
            return "".component
        }

        if (allMessages.size == 1) {
            return allMessages.first()
        }

        return allMessages.reduceRight { component, acc ->
            component.append(divider).append(acc)
        }
    }

    fun tick() {
        ticker++
        temporaryMessages.removeIf { (tick, text, important) -> ticker >= tick }
    }

    fun sendTemporaryMessage(component: TextComponent, duration: Int, important: Boolean = true) {
        temporaryMessages.add(Triple(duration, component, important))
    }

    fun addMessage(key: NamespacedKey, message: TextComponent, important: Boolean = false) {
        messages[key] = message to important
    }

    // Only changes the message if it exists
    fun updateMessage(key: NamespacedKey, message: TextComponent, important: Boolean = false) {
        if (messages.containsKey(key)) {
            messages[key] = message to important
        }
    }

    fun removeMessage(key: NamespacedKey) {
        messages.remove(key)
    }
}

class Waypoints {
    private var ticker = 0

    var trackedEntities = mutableListOf<Entity>()
    var trackedLocations = mutableListOf<Location>()

    fun tick() {
        ticker++
//        temporaryMessages.removeIf { (tick, text, important) -> ticker >= tick }
    }

    fun getPacket() {

    }
}

class PlayerCommunicationManager {
    val actionBar = ActionBar()
    val waypoints = Waypoints()

    fun tick() {
        actionBar.tick()
    }
}