package io.github.metrolung.traitorgame.trait

import io.github.metrolung.traitorgame.Colors
import io.github.metrolung.traitorgame.MiniMessage
import io.github.metrolung.traitorgame.TraitorGamePlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import net.kyori.adventure.title.TitlePart
import org.bukkit.EntityEffect
import org.bukkit.Material
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import xyz.xenondevs.invui.item.ItemBuilder
import java.time.Duration

class LastChance(
    var used: Boolean = false
) : Trait {
    override fun onTakeDamage(event: Trait.Event, damage: Trait.DamageInfo) {
        if (!damage.isKilling) {
            return
        }

        if (damage.attackingPlayer != null) {
            return
        }

        if (event.isCancelled) {
            return
        }

        used = true
        event.isCancelled = true
        event.isHandled = true

        event.player.actualPlayer?.sendTitlePart(
            TitlePart.TIMES,
            Title.Times.times(
                Duration.ofMillis(100),
                Duration.ofMillis(1500),
                Duration.ofSeconds(1)
            )
        )

        event.player.actualPlayer?.sendTitlePart(
            TitlePart.TITLE,
            MiniMessage.deserialize("<gradient:#88f0fc:#ccffff><b>Last chance!</b></gradient>")
        )

        event.player.actualPlayer?.playEffect(EntityEffect.PROTECTED_FROM_DEATH)

        event.player.actualPlayer?.health = 5.0
        event.player.actualPlayer?.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, 200, 2, true, true))
        event.player.actualPlayer?.addPotionEffect(PotionEffect(PotionEffectType.WEAKNESS, 200, 2, true, true))
        event.player.actualPlayer?.addPotionEffect(PotionEffect(PotionEffectType.GLOWING, 200, 0, true, true))
        event.player.actualPlayer?.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 100, 0, true, true))
    }

    override val settings: Trait.Settings
        get() = SETTINGS

    companion object {
        val SETTINGS = Trait.Settings(
            key = TraitorGamePlugin.key("last_chance"),
            name = "Last Chance",
            traitColor = Colors.LAST_CHANCE,
            stylized = Component.textOfChildren(MiniMessage.deserialize("<gradient:#88f0fc:#ccffff><b>Last chance!</b></gradient>")),
            itemProvider = ItemBuilder(Material.TOTEM_OF_UNDYING),
            builder = { LastChance() }
        )
    }
}