package io.github.metrolung.traitorgame

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import org.bukkit.NamespacedKey
import xyz.xenondevs.invui.inventory.VirtualInventory

class StatusBar(val divider: Component) {
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

        val allMessages = notImportant.subList(0, notImportant.size/2) + important + notImportant.subList(notImportant.size/2, notImportant.size)

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

    fun tick(): TextComponent {
        ticker++
        temporaryMessages.removeIf { (tick, text, important) -> ticker >= tick }
        return getMessage()
    }

    fun sendTemporaryMessage(component: TextComponent, duration: Int, important: Boolean = true) {
        temporaryMessages.add(Triple(duration, component, important))
    }

    fun sendMessage(key: NamespacedKey, message: TextComponent, important: Boolean = false) {
        messages[key] = message to important
    }

    // Only changes the message if it exists
    fun updateMessage(key: NamespacedKey, message: TextComponent, important: Boolean = false) {
        if (messages.containsKey(key)) {
            messages[key] = message to important
        }
    }

    fun hideMessage(key: NamespacedKey) {
        messages.remove(key)
    }
}