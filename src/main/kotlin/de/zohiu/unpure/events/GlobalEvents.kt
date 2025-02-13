package de.zohiu.unpure.events

import de.zohiu.unpure.UnPure
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerChangedWorldEvent

class GlobalEvents : Listener {
    @EventHandler
    fun onPlayerChangeWorld(ev: PlayerChangedWorldEvent) {
        // Return here because on this server, we only ever have one game.
        return
        val player = ev.player

        Bukkit.getOnlinePlayers().forEach { op ->
            if (player.world != op.world) {
                player.hidePlayer(op)
                op.hidePlayer(player)
            } else {
                player.showPlayer(op)
                op.showPlayer(player)
            }
        }
    }
}