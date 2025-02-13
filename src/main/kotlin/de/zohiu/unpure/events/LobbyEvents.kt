package de.zohiu.unpure.events

import de.zohiu.unpure.UnPure
import de.zohiu.unpure.config.Config
import de.zohiu.unpure.game.Game
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent

class LobbyEvents : Listener {
    init {
        // Parkour auto reset
        UnPure.crimson.effectBuilder().repeatForever(10) {
            Bukkit.getOnlinePlayers().forEach {
                if (it.world == UnPure.waitingarea) {
                    if (it.location.y < 69) {
                        val loc = UnPure.waitingarea.spawnLocation
                        loc.add(0.5, 0.0, 0.5)
                        loc.yaw = -90.0f
                        it.teleport(loc)
                    }
                }
            }
        }.start()
    }

    // Block any block breaks
    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        if (event.player.world != UnPure.lobby && event.player.world != UnPure.waitingarea) { return; }
        if (!event.player.hasPermission("unpure.lobbyinteract")) event.isCancelled = true
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        if (event.player.world != UnPure.lobby && event.player.world != UnPure.waitingarea) { return; }
        if (!event.player.hasPermission("unpure.lobbyinteract")) event.isCancelled = true
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (event.player.world != UnPure.lobby && event.player.world != UnPure.waitingarea) { return; }
        if (!event.player.hasPermission("unpure.lobbyinteract")) event.isCancelled = true
    }

    @EventHandler
    fun onDamage(event: EntityDamageByEntityEvent) {
        if (event.entity.world != UnPure.lobby && event.entity.world != UnPure.waitingarea) { return; }
        event.isCancelled = true
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val loc = Bukkit.getWorld(UnPure.lobbyMapPath)!!.spawnLocation
        loc.add(0.5, 0.0, 0.5)
        loc.yaw = -90.0f
        event.player.teleport(loc)
        event.player.inventory.clear()
    }
}