package io.github.metrolung.traitorgame

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Location
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.util.Transformation
import org.geysermc.floodgate.api.FloodgateApi
import org.joml.AxisAngle4f
import org.joml.Vector3f

val FloodgateApi: FloodgateApi
    get() = org.geysermc.floodgate.api.FloodgateApi.getInstance()

val MiniMessage: MiniMessage
    get() = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()


fun EquipmentSlot.getIndex(player: Player): Int {
    return when (this) {
        EquipmentSlot.HAND -> player.inventory.heldItemSlot
        EquipmentSlot.OFF_HAND -> 40
        EquipmentSlot.FEET -> 36
        EquipmentSlot.LEGS -> 37
        EquipmentSlot.CHEST -> 38
        EquipmentSlot.HEAD -> 39
        EquipmentSlot.BODY -> 41
        EquipmentSlot.SADDLE -> -1
    }
}

fun Transformation(
    translation: Vector3f = Vector3f(),
    leftRotation: AxisAngle4f = AxisAngle4f(),
    scale: Vector3f = Vector3f(1f, 1f, 1f),
    rightRotation: AxisAngle4f = AxisAngle4f()
): Transformation = Transformation(translation, leftRotation, scale, rightRotation)

fun Location.isSameBlockAs(other: Location): Boolean
    = this.world.key == other.world.key
        && this.blockX == other.blockX
        && this.blockY == other.blockY
        && this.blockZ == other.blockZ

fun Player.resetStats(
    includeAdvancements: Boolean = true,
    includeInventory: Boolean = true,
    includeEnderChest: Boolean = includeInventory
) {
    health = getAttribute(Attribute.MAX_HEALTH)!!.attribute.defaultValue
    foodLevel = 20
    saturation = 5.0f

    level = 0
    exp = 0f
    totalExperience = 0

    clearActivePotionEffects()

    if (includeInventory)
        inventory.clear()

    if (includeEnderChest)
        enderChest.clear()

    if (includeAdvancements)
        for (advancement in server.advancementIterator()) {
            val progress = getAdvancementProgress(advancement)
            for (criteria in progress.awardedCriteria) {
                progress.revokeCriteria(criteria)
            }
        }
}