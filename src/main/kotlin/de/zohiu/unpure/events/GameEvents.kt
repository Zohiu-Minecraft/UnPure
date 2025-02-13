package de.zohiu.unpure.events

import de.zohiu.unpure.game.Game
import org.bukkit.Material
import org.bukkit.entity.Firework
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack


class GameEvents(val game: Game) : Listener {
    @EventHandler
    fun onAttack(event: EntityDamageByEntityEvent) {
        if (!game.inProgress) { return }
        if (event.entity.world != game.world) { return; }

        if (event.damager !is Player || event.entity !is Player) { return }
        val attacker: Player = event.damager as Player
        val victim: Player = event.entity as Player
    }

    @EventHandler
    fun onDeath(event: EntityDamageEvent) {
        if (!game.inProgress) { return }
        if (event.entity.world != game.world) { return; }
        if (event.entity !is Player) { return }

        val victim: Player = event.entity as Player
        if (victim.health - event.damage > 0) {
            return
        }
        event.isCancelled = true
        victim.health = 20.0
        victim.foodLevel = 20

        val cause: EntityDamageEvent? = victim.lastDamageCause
        if (cause is EntityDamageByEntityEvent && cause.damager is Player) {
            game.infectPlayer(victim, cause.damager as Player)
            return
        }

        game.infectPlayer(victim)
    }

    @EventHandler
    fun stopFireworkDamage(event: EntityDamageByEntityEvent) {
        if (event.damager is Firework) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onLeave(event: PlayerQuitEvent) {
        if (!game.inProgress) { return }
        if (event.player.world != game.world) { return; }
        game.removePlayer(event.player)
    }

    // Lock players in map
    // Block break stuff like autosmelt
    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        if (!game.inProgress) { return }
        if (event.player.world != game.world) { return; }
        val block_type = event.block.type

        if (block_type == Material.WARPED_STEM) {
            event.isCancelled = true
            return
        }
        val smelted = when (block_type) {
            Material.IRON_ORE -> Material.IRON_INGOT
            Material.GOLD_ORE -> Material.GOLD_INGOT
            else -> null
        }

        if (smelted != null) {
            event.block.type = Material.AIR
            event.block.world.dropItemNaturally(event.block.location, ItemStack(smelted, 1))
        }
    }

    // Lock armor slots and crafting for infected
    @EventHandler
    fun onInventory(event: InventoryClickEvent) {
        if (!game.inProgress) { return }
        if (event.whoClicked.world != game.world) { return; }
        val player = event.whoClicked as Player

        if (game.infected.contains(player)) {
            if (event.slotType == InventoryType.SlotType.ARMOR || event.slotType == InventoryType.SlotType.CRAFTING) {
                event.isCancelled = true
            }
        }
    }
}