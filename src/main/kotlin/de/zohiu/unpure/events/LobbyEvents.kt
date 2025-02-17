package de.zohiu.unpure.events

import de.zohiu.unpure.UnPure
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.potion.PotionEffectType

class LobbyEvents : Listener {
    init {
        // Parkour auto reset
        UnPure.crimson.effectBuilder().repeatForever(10) {
            Bukkit.getOnlinePlayers().forEach {
                if (it.world == UnPure.lobby) {
                    if (it.gameMode != GameMode.ADVENTURE && it.gameMode != GameMode.CREATIVE) {
                        it.gameMode = GameMode.ADVENTURE
                        it.removePotionEffect(PotionEffectType.GLOWING)
                        val loc = UnPure.lobby.spawnLocation.add(0.5, 0.0, 0.5)
                        it.teleport(loc)
                    }
                }
            }
        }.start()
    }

    // Block any block breaks
    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        if (event.player.world != UnPure.lobby) { return; }
        if (!event.player.hasPermission("unpure.lobbyinteract")) event.isCancelled = true
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        if (event.player.world != UnPure.lobby) { return; }
        if (!event.player.hasPermission("unpure.lobbyinteract")) event.isCancelled = true
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (event.player.world != UnPure.lobby) { return; }
        if (event.hasBlock() && event.action == Action.PHYSICAL && event.clickedBlock!!.type == Material.FARMLAND) event.isCancelled = true
        if (!event.player.hasPermission("unpure.lobbyinteract")) event.isCancelled = true
    }

    @EventHandler
    fun onDamage(event: EntityDamageByEntityEvent) {
        if (event.entity.world != UnPure.lobby) { return; }
        event.isCancelled = true
    }
}