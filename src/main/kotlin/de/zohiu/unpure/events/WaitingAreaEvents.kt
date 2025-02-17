package de.zohiu.unpure.events

import de.zohiu.unpure.UnPure
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEvent

class WaitingAreaEvents : Listener {
    @EventHandler
    fun onWaitingAreaBlockBreak(event: BlockBreakEvent) {
        if (event.player.world != UnPure.waitingArea) { return; }
        event.isCancelled = true
    }

    @EventHandler
    fun onWaitingAreaBlockPlace(event: BlockPlaceEvent) {
        if (event.player.world != UnPure.waitingArea) { return; }
        event.isCancelled = true
    }

    @EventHandler
    fun onWaitingAreaInteract(event: PlayerInteractEvent) {
        if (event.player.world != UnPure.waitingArea) { return; }
        event.isCancelled = true
    }

    @EventHandler
    fun onWaitingAreaDamage(event: EntityDamageByEntityEvent) {
        if (event.entity.world != UnPure.waitingArea) { return; }
        event.isCancelled = true
    }
}