package de.zohiu.unpure.events

import de.zohiu.unpure.UnPure
import de.zohiu.unpure.config.Config
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.AsyncPlayerChatEvent

class LobbyEvents : Listener {
    @EventHandler
    fun onChat(event: AsyncPlayerChatEvent) {
        if (event.player.world != UnPure.lobby) { return; }
        event.isCancelled = true
        val player = event.player

        UnPure.lobby.players.forEach {
            it.sendMessage(Config.str("chat.format.general", arrayOf(player.name, event.message)))
        }
    }

    // Block any block breaks
    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        if (event.player.world != UnPure.lobby) { return; }
        event.isCancelled = true
    }
}