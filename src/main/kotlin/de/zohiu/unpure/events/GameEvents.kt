package de.zohiu.unpure.events

import com.destroystokyo.paper.event.player.PlayerJumpEvent
import de.zohiu.unpure.UnPure
import de.zohiu.unpure.game.Game
import de.zohiu.unpure.data.DataOperations
import de.zohiu.unpure.data.StatisticsData
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.*
import org.bukkit.inventory.ItemStack


class GameEvents(val game: Game) : Listener {
    @EventHandler
    fun hideOutsideMessages(event: AsyncPlayerChatEvent) {
        if (!game.inProgress) {
            return
        }
        // Spectator messages are allowed and stay inside the game
        if (game.spectators.contains(event.player)) {
            game.rawBroadcast(ChatColor.translateAlternateColorCodes(
                '&', "&7&lSpectator &r&7${event.player.name}&f: ") + event.message, game.players)
            event.isCancelled = true
            return
        }

        if (event.player.world != UnPure.waitingArea && event.player.world != game.world) {
            // Hide all outside messages that don't come from a game world
            event.recipients.removeIf { game.players.contains(it) }
        }
    }

    @EventHandler
    fun onChat(event: AsyncPlayerChatEvent) {
        if (!game.inProgress) {
            return
        }
        if (event.player.world != game.world) { return; }

        // Disable friendly fire
        if (game.infected.contains(event.player)) {
            event.isCancelled = true
            game.rawBroadcast(ChatColor.translateAlternateColorCodes(
                '&', "&2&lInfected &r&2${event.player.name}&f: ") + event.message, game.players)
            return
        }
        if (game.humans.contains(event.player)) {
            event.isCancelled = true
            game.rawBroadcast(ChatColor.translateAlternateColorCodes(
                '&', "&e&lHuman &r&e${event.player.name}&f: ") + event.message, game.players)
            return
        }
    }

    // Waitingarea chat
    @EventHandler
    fun onWaitingChat(event: AsyncPlayerChatEvent) {
        if (!game.inProgress) {
            return
        }
        if (event.player.world != UnPure.waitingArea) { return; }
        if (!game.players.contains(event.player)) { return; }
        event.isCancelled = true
        game.rawBroadcast(ChatColor.translateAlternateColorCodes(
            '&', "&2&lInfected &r&2${event.player.name}&f: ") + event.message, game.players)
    }

    @EventHandler
    fun onHit(event: EntityDamageByEntityEvent) {
        if (!game.inProgress) {
            event.isCancelled = true
            return
        }
        if (event.entity.world != game.world) { return; }
        if (event.entity !is Player) { return }
        val victim = event.entity
        val attacker = event.damager
        if (attacker !is Player || victim !is Player) return

        // Disable friendly fire
        if (game.infected.contains(attacker) && game.infected.contains(victim)) {
            event.isCancelled = true
            return
        }
        if (game.humans.contains(attacker) && game.humans.contains(victim)) {
            event.isCancelled = true
            return
        }

        DataOperations.addNToTable(attacker, StatisticsData.tableDamageDealt, event.damage.toInt())
    }

    @EventHandler
    fun onDamage(event: EntityDamageEvent) {
        if (!game.inProgress) {
            event.isCancelled = true
            return
        }
        if (event.entity.world != game.world) { return; }
        if (event.entity !is Player) { return }

        val victim: Player = event.entity as Player
        val cause: EntityDamageEvent? = victim.lastDamageCause
        var attackerPlayer: Player? = null
        if (cause is EntityDamageByEntityEvent && cause.damager is Player) {
            attackerPlayer = cause.damager as Player
        }

        DataOperations.addNToTable(victim, StatisticsData.tableDamageTaken, event.damage.toInt())

        if (victim.health - event.damage > 0) { return  }
        event.isCancelled = true
        victim.health = 20.0
        victim.foodLevel = 20

        if (attackerPlayer != null) {
            game.infectPlayer(victim, attackerPlayer)
            return
        }

        game.infectPlayer(victim)
    }

    // Lock players in map
    // Block break stuff like autosmelt
    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        if (!game.inProgress) { return }
        if (event.player.world != game.world) { return; }
        DataOperations.addOneToTable(event.player, StatisticsData.tableBlocksMined)
        val block_type = event.block.type

        if (
            Tag.COAL_ORES.isTagged(block_type)
            || Tag.GOLD_ORES.isTagged(block_type)
            || Tag.IRON_ORES.isTagged(block_type)
            || Tag.LAPIS_ORES.isTagged(block_type)
            || Tag.COPPER_ORES.isTagged(block_type)
            || Tag.DIAMOND_ORES.isTagged(block_type)
            || Tag.EMERALD_ORES.isTagged(block_type)
            || Tag.REDSTONE_ORES.isTagged(block_type)
        ) {
            DataOperations.addOneToTable(event.player, StatisticsData.tableOresMined)
        }

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

    // Events just for statistics
    @EventHandler
    fun onJump(event: PlayerJumpEvent) {
        if (!game.inProgress) { return }
        if (event.player.world != game.world) { return; }
        DataOperations.addOneToTable(event.player, StatisticsData.tableJumps)
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        if (!game.inProgress) { return }
        if (event.player.world != game.world) { return; }
        DataOperations.addOneToTable(event.player, StatisticsData.tableBlocksPlaced)
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        UnPure.updatePlaytime(event.player)
        UnPure.playtimeCounter.remove(event.player)

        // Hide new player for game players
        game.players.forEach {
            it.hidePlayer(UnPure.instance, event.player)
        }
    }
}