package de.zohiu.unpure.gui

import de.zohiu.crimson.GUI
import de.zohiu.unpure.data.CosmeticsData
import de.zohiu.unpure.game.*
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

class CosmeticsGUI(player: Player) : GUI(player) {
    var page: Int = 0

    // Cosmetics: Death effect, win effect, infected outfit, trail, kill message

    override fun render(): Inventory {
        val unlocked: MutableList<CosmeticType>
        val equipped: CosmeticType?
        val inventory = when (page) {
            0 -> {
                val inventory = Bukkit.createInventory(null, 9, "Cosmetics Menu")
                inventory.setItem(2, item(Material.REDSTONE, "${ChatColor.RED}Death effects"))
                registerButton(2) { page = 1 }
                inventory.setItem(3, item(Material.EMERALD, "${ChatColor.GREEN}Win effects"))
                registerButton(3) { page = 2 }
                inventory.setItem(4, item(Material.ZOMBIE_HEAD, "${ChatColor.DARK_GREEN}Outfits"))
                registerButton(4) { page = 3 }
                inventory.setItem(5, item(Material.MAGENTA_DYE, "${ChatColor.LIGHT_PURPLE}Trails"))
                registerButton(5) { page = 4 }
                inventory.setItem(6, item(Material.OAK_SIGN, "${ChatColor.GOLD}Kill messages"))
                registerButton(6) { page = 5 }
                return inventory
            }

            1 -> {
                val inventory = Bukkit.createInventory(null, 9 * 5, "Death effects")
                unlocked = CosmeticsData.getUnlockedCosmetics(player, DeathEffect.values().toList())
                equipped = CosmeticsData.getEquippedCosmetic(player, DeathEffect::class.simpleName!!, DeathEffect.DEFAULT)
                inventory
            }
            2 -> {
                val inventory = Bukkit.createInventory(null, 9 * 5, "Win effects")
                unlocked = CosmeticsData.getUnlockedCosmetics(player, WinEffect.values().toList())
                equipped = CosmeticsData.getEquippedCosmetic(player, WinEffect::class.simpleName!!, WinEffect.DEFAULT)
                inventory
            }
            3 -> {
                val inventory = Bukkit.createInventory(null, 9 * 5, "Outfits")
                unlocked = CosmeticsData.getUnlockedCosmetics(player, InfectedOutfit.values().toList())
                equipped = CosmeticsData.getEquippedCosmetic(player, InfectedOutfit::class.simpleName!!, InfectedOutfit.DEFAULT)
                inventory
            }
            4 -> {
                val inventory = Bukkit.createInventory(null, 9 * 5, "Trails")
                unlocked = CosmeticsData.getUnlockedCosmetics(player, Trail.values().toList())
                equipped = CosmeticsData.getEquippedCosmetic(player, Trail::class.simpleName!!, Trail.DEFAULT)
                inventory
            }
            5 -> {
                val inventory = Bukkit.createInventory(null, 9 * 5, "Kill messages")
                unlocked = CosmeticsData.getUnlockedCosmetics(player, KillMessage.values().toList())
                equipped = CosmeticsData.getEquippedCosmetic(player, KillMessage::class.simpleName!!, KillMessage.DEFAULT)
                inventory
            }

            else -> { return Bukkit.createInventory(null, 9, "none") }
        }

        var slot = 0
        unlocked.forEach {
            val mat = if (it == equipped) Material.LIME_DYE
            else Material.GRAY_DYE
            val col = if (it == equipped) ChatColor.GREEN
            else ChatColor.GRAY

            val rarityColor = when(it.rarity) {
                Rarity.DEFAULT -> ChatColor.GRAY
                Rarity.COMMON -> ChatColor.WHITE
                Rarity.RARE -> ChatColor.AQUA
                Rarity.EPIC -> ChatColor.DARK_PURPLE
                Rarity.LEGENDARY -> ChatColor.GOLD
            }

            val rarityItem = if (it == equipped) Material.LIME_DYE
            else when(it.rarity) {
                Rarity.DEFAULT -> Material.GRAY_DYE
                Rarity.COMMON -> Material.WHITE_DYE
                Rarity.RARE -> Material.NETHER_STAR
                Rarity.EPIC -> Material.AMETHYST_SHARD
                Rarity.LEGENDARY -> Material.NETHERITE_INGOT
            }

            inventory.setItem(slot,
                item(rarityItem, "${col}${it}", arrayListOf(
                    ChatColor.translateAlternateColorCodes('&', "&r&7${it.description}"),
                    "",
                    "${ChatColor.GRAY}Rarity: ${rarityColor}${ChatColor.BOLD}${it.rarity}")))

            registerButton(slot) {
                CosmeticsData.equipCosmetic(player, it)
            }
            slot++
        }

        inventory.setItem(9 * 4 + 3, item(Material.ARROW, "${ChatColor.WHITE}Last page"))
        inventory.setItem(9 * 4 + 4, item(Material.BARRIER, "${ChatColor.RED}Back to menu"))
        registerButton(9 * 4 + 4) { page = 0 }
        inventory.setItem(9 * 4 + 5, item(Material.ARROW, "${ChatColor.WHITE}Next page"))

        return inventory
    }
}