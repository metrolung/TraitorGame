package io.github.metrolung.traitorgame

import com.destroystokyo.paper.profile.ProfileProperty
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ResolvableProfile
import io.papermc.paper.datacomponent.item.TooltipDisplay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BundleMeta
import org.bukkit.persistence.PersistentDataType
import java.util.UUID


object ItemStacks {
    val OBSCURED_KEY = TraitorGamePlugin.key("obscured")
    val ROLE_EQUIPMENT_KEY = TraitorGamePlugin.key("equipment")
    val ROLE_MATERIAL_KEY = TraitorGamePlugin.key("material")

    fun manifesto(codeword: String): ItemStack = ItemStack.of(Material.BOOK).apply {
            this.setData(DataComponentTypes.ITEM_NAME, Component.text("Traitor's Manifesto").color(Colors.TRAITOR_RED.textColor))
            this.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            this.lore(
                listOf(
                    "DO NOT LET ANYONE SEE THIS"
                        .colored(Colors.VERY_YELLOW)
                        .decorate(TextDecoration.BOLD)
                        .decoration(TextDecoration.ITALIC, false),
                    "Code Word: $codeword"
                        .colored(Colors.WHITE)
                        .decoration(TextDecoration.ITALIC, false)
                )
            )
            this.editPersistentDataContainer { pdc ->
                pdc.set(ROLE_MATERIAL_KEY, PersistentDataType.STRING, "manifesto")
            }
        }

    val notebook: ItemStack
        get() = ItemStack.of(Material.BOOK).apply {
            this.setData(DataComponentTypes.ITEM_NAME, Component.text("Notebook"))
            this.setData(DataComponentTypes.ITEM_MODEL, NamespacedKey.minecraft("writable_book"))
            this.lore(listOf("Shift + click to open".colored(Colors.VERY_YELLOW)))
            this.editPersistentDataContainer { pdc ->
                pdc.set(ROLE_EQUIPMENT_KEY, PersistentDataType.STRING, "notebook")
            }
        }

    val merchandiseBox: ItemStack
        get() = ItemStack.of(Material.CHEST).apply {
            this.setData(DataComponentTypes.ITEM_NAME, Component.text("Merchandise Box"))
            this.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            this.lore(listOf("Click to open".colored(Colors.VERY_YELLOW)))
            this.editPersistentDataContainer { pdc ->
                pdc.set(ROLE_EQUIPMENT_KEY, PersistentDataType.STRING, "merchandise_box")
                pdc.set(OBSCURED_KEY, PersistentDataType.BOOLEAN, true)
            }
        }

    val sampleChecker: ItemStack
        get() = ItemStack.of(Material.WHITE_SHULKER_BOX).apply {
            this.setData(DataComponentTypes.ITEM_NAME, Component.text("Sample Checker"))
            this.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            this.lore(listOf("Click to open".colored(Colors.VERY_YELLOW)))
            this.editPersistentDataContainer { pdc ->
                pdc.set(ROLE_EQUIPMENT_KEY, PersistentDataType.STRING, "sample_checker")
                pdc.set(OBSCURED_KEY, PersistentDataType.BOOLEAN, true)
            }
        }

    val swab: ItemStack
        get() = ItemStack.of(Material.WHITE_DYE).apply {
            this.setData(DataComponentTypes.ITEM_NAME, "Swab".component)
            this.setData(DataComponentTypes.ITEM_MODEL, NamespacedKey.minecraft("bone"))
            this.lore(listOf("Click a player to swab them".colored(Colors.VERY_YELLOW)))
            this.editPersistentDataContainer { pdc ->
                pdc.set(ROLE_MATERIAL_KEY, PersistentDataType.STRING, "swab")
            }
        }

    val SAMPLE_KEY = TraitorGamePlugin.key("used_swab")
    fun usedSwab(name: Component, index: Int) = ItemStack.of(Material.WHITE_DYE).apply {
        this.setData(DataComponentTypes.ITEM_NAME, name)
        this.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
        this.setData(DataComponentTypes.ITEM_MODEL, NamespacedKey.minecraft("bone"))
        this.editPersistentDataContainer { pdc ->
            pdc.set(ROLE_MATERIAL_KEY, PersistentDataType.STRING, "used_swab")
            pdc.set(SAMPLE_KEY, PersistentDataType.INTEGER, index)
        }
    }

    val detectiveCrossbow: ItemStack
        get() = ItemStack.of(Material.CROSSBOW).apply {
            this.editMeta { meta ->
                meta.addEnchant(Enchantment.QUICK_CHARGE, 2, true)
            }
        }

    fun bundle(arrows: MutableList<ItemStack>): ItemStack = ItemStack.of(Material.BUNDLE).apply {
        println(arrows)
        this.editMeta { meta ->
            (meta as BundleMeta).setItems(arrows)
        }
    }


//    val MERCHANDISE_INDEX_KEY = TraitorGamePlugin.key("merchandise_index")
    fun merchandise(name: Component, amount: Int, material: Material) = ItemStack.of(material).apply {
        this.setData(DataComponentTypes.ITEM_NAME, name)
        this.setData(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
        this.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().addHiddenComponents(
            DataComponentTypes.BUNDLE_CONTENTS))
        this.setData(DataComponentTypes.MAX_STACK_SIZE, amount)
        this.amount = amount
        this.lore(listOf("Click twice to give to someone".colored(Colors.VERY_YELLOW)))
//        this.editPersistentDataContainer { pdc ->
//            pdc.set(MERCHANDISE_INDEX_KEY, PersistentDataType.INTEGER, index)
//        }
    }

    object Gui {
        fun identifiedPlayer(name: Component, player: UUID): ItemStack
            = ItemStack.of(Material.PLAYER_HEAD).apply {
                this.setData(DataComponentTypes.ITEM_NAME, name)
                this.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().addHiddenComponents(
                    DataComponentTypes.PROFILE))
                this.setData(
                    DataComponentTypes.PROFILE,
                    ResolvableProfile.resolvableProfile()
                        .uuid(player)
                )
            }

        val unidentifiedPlayerBedrock: ItemStack
            get() = ItemStack.of(Material.COAL).apply {
                this.setData(DataComponentTypes.ITEM_NAME, "Unidentified Player".component)
                this.lore(listOf(
                    "Scan this player's swab to identify them".colored(Colors.VERY_YELLOW)
                ))
            }

        val unidentifiedPlayer: ItemStack
            get() = ItemStack.of(Material.PLAYER_HEAD).apply {
                this.setData(DataComponentTypes.ITEM_NAME, "Unidentified Player".component)
                this.lore(listOf(
                    "Scan this player's swab to identify them".colored(Colors.VERY_YELLOW)
                ))
                this.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().addHiddenComponents(
                    DataComponentTypes.PROFILE))
                this.setData(
                    DataComponentTypes.PROFILE,
                    ResolvableProfile.resolvableProfile()
                        .addProperty(ProfileProperty(
                            "textures",
                            "ewogICJ0aW1lc3RhbXAiIDogMTYyMjIxNTgxMzMyMywKICAicHJvZmlsZUlkIiA6ICI0ZTMwZjUwZTdiYWU0M2YzYWZkMmE3NDUyY2ViZTI5YyIsCiAgInByb2ZpbGVOYW1lIiA6ICJfdG9tYXRvel8iLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODBjYmRkNGY0NTRmM2U4MzU5YmNjMmI0Y2I4Njk1NWYwZDZkNmM1OGY5Y2E0ZDNjMGI2ZDkzNjRkMTAwYTljNiIKICAgIH0KICB9Cn0=",
                            "VhpsnOF3KctvjkmwMrnJrAUXT9VRk2h80ekcrghXHw/8HtHIX1C3BUJ+Jq5WHPN7C36LyhHOAvpBRKM2pPilbk3Ry7McJoPWHi1D4Xs2/7qWYJIFxfOSnF36hD01zgyuomk9Tu09n8QDZzumvlL6CchzaFxgpavJB8SknoE+0Aa21aeZPSfNxjCDjAAcZH/fmUBSEDSdRIAXQbQXs1jfsdRRYiDzArlaQQ1yK2pucwsnoF1NzmcD7wQf+wn9jzTDUGsX3hEjCnsCHgR2gVEb9v3m6uuYjN9EFRdJS67i3TivhHW95D04zeC6KvYimPGymmGIWjn8b/KN+ucze3XUJfrnuFW/TtYzpOF9gwd6CnZr/m5jttEsQ/56xATdYd6pIIpV4EBtKDhmtvs2ZGo606KZAszS8r9J0M9itMGiSgKFDShwsutwctlOHGAGD21CD2n5Q55O79Xhr53EQ/BOWGsGGvfhxL1kkjc0n3QIg8D4ySYotDvC/iWPMa8LceWUftG9nY52mH95ZFa0wf+I1Chxb7qsl33ouzuXGXuTV0SgfUmKaSG00ROmJfAcO0JBGBdE+dQdOCiAPiU2Xu72znaaxYB90RRxfFTAg3B+0qZtMzoloFHqnVsJnWLYpYpA+8RaR0TAyBCbB9kettWk8w/u9bNc10XZi9Z1k0s9xcM="
                        ))
                )
            }

        val blackGlass
            get() = ItemStack.of(Material.BLACK_STAINED_GLASS_PANE).apply {
                this.setData(DataComponentTypes.ITEM_NAME, "".component)
                this.editMeta { it.isHideTooltip = true }
            }
        val whiteGlass
            get() = ItemStack.of(Material.WHITE_STAINED_GLASS_PANE).apply {
                this.setData(DataComponentTypes.ITEM_NAME, "".component)
                this.editMeta { it.isHideTooltip = true }
            }
        val yellowGlass
            get() = ItemStack.of(Material.YELLOW_STAINED_GLASS_PANE).apply {
                this.setData(DataComponentTypes.ITEM_NAME, "".component)
                this.editMeta { it.isHideTooltip = true }
            }
        val limeGlass
            get() = ItemStack.of(Material.YELLOW_STAINED_GLASS_PANE).apply {
                this.setData(DataComponentTypes.ITEM_NAME, "".component)
                this.editMeta { it.isHideTooltip = true }
            }
        val barrier
            get() = ItemStack.of(Material.BARRIER).apply {
                this.setData(DataComponentTypes.ITEM_NAME, "".component)
                this.editMeta { it.isHideTooltip = true }
            }
    }

}