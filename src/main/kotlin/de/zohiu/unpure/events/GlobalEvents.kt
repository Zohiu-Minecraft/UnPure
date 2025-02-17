package de.zohiu.unpure.events

import de.zohiu.unpure.UnPure
import de.zohiu.unpure.commands.general.AFK
import de.zohiu.unpure.data.DataOperations
import de.zohiu.unpure.data.StatisticsData
import de.zohiu.unpure.game.Game
import org.bukkit.Bukkit
import org.bukkit.entity.Firework
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.*

class GlobalEvents : Listener {
    val lastMove: HashMap<Player, Long> = HashMap()

    init {
        // AFK code
        UnPure.crimson.effectBuilder().repeatForever(20) {
            Bukkit.getOnlinePlayers().forEach {
                val now = System.currentTimeMillis()
                if (!lastMove.contains(it)) lastMove[it] = now
                if (now - lastMove[it]!! > 2 * 60 * 1000) {
                    AFK.setAFK(it, true)
                }
            }
        }.start()
    }

    @EventHandler
    fun stopFireworkDamage(event: EntityDamageByEntityEvent) {
        if (event.damager is Firework) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val loc = Bukkit.getWorld(UnPure.lobbyMapPath)!!.spawnLocation
        loc.add(0.5, 0.0, 0.5)
        loc.yaw = -90.0f
        event.player.teleport(loc)
        event.player.inventory.clear()
        DataOperations.addOneToTable(event.player, StatisticsData.tableJoins)
        UnPure.playtimeCounter[event.player] = System.currentTimeMillis()

        if (DataOperations.getTableLong(event.player, StatisticsData.tableFirstJoin) == 0L) {
            // never joined before
            DataOperations.addNToTable(event.player, StatisticsData.tableFirstJoin, UnPure.playtimeCounter[event.player]!!)
        }

        AFK.setAFK(event.player, false)
    }

    @EventHandler
    fun onLeave(event: PlayerQuitEvent) {
        UnPure.updatePlaytime(event.player)
        UnPure.playtimeCounter.remove(event.player)

        // Remove player from their game
        Game.openGames.forEach {
            if (it.players.contains(event.player)) it.removePlayer(event.player)
        }
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        lastMove[event.player] = System.currentTimeMillis()
        AFK.setAFK(event.player, false)
    }

    @EventHandler
    fun onCommand(event: PlayerCommandSendEvent) {
        AFK.setAFK(event.player, false)
    }

    @EventHandler
    fun onWorldChange(event: PlayerChangedWorldEvent) {
        // Ignore event if player is in a game
        for (game in Game.openGames) {
            if (game.players.contains(event.player)) return
            if (game.spectators.contains(event.player)) return
        }

        // Per-world player list
        Bukkit.getOnlinePlayers().forEach { op ->
            if (event.player.world != op.world) {
                event.player.hidePlayer(UnPure.instance, op)
                op.hidePlayer(UnPure.instance, event.player)
            } else {
                event.player.showPlayer(UnPure.instance, op)
                op.showPlayer(UnPure.instance, event.player)
            }
        }
    }
}