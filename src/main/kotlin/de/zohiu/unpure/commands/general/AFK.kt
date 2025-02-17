package de.zohiu.unpure.commands.general

import me.neznamy.tab.api.TabAPI
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class AFK : CommandExecutor {
    init {
        Bukkit.getOnlinePlayers().forEach { player ->
            TabAPI.getInstance().getPlayer(player.uniqueId)?.let { TabAPI.getInstance().tabListFormatManager?.setSuffix(it, null) }
            TabAPI.getInstance().getPlayer(player.uniqueId)?.let { TabAPI.getInstance().nameTagManager?.setSuffix(it, null) }
            TabAPI.getInstance().getPlayer(player.uniqueId)?.setTemporaryGroup(null)
        }
    }

    companion object {
        val afkMap: HashMap<Player, Boolean> = HashMap()

        fun setAFK(player: Player, value: Boolean) {
            if (value && !afkMap.contains(player)) {
                afkMap[player] = true
                player.sendMessage("${ChatColor.GRAY}You are now AFK.")

                TabAPI.getInstance().getPlayer(player.uniqueId)?.let { TabAPI.getInstance().tabListFormatManager?.setSuffix(it, " &8&o*afk*") }
                TabAPI.getInstance().getPlayer(player.uniqueId)?.let { TabAPI.getInstance().nameTagManager?.setSuffix(it, " &8&o*afk*") }
                TabAPI.getInstance().getPlayer(player.uniqueId)?.setTemporaryGroup("afk")

            } else if (!value && afkMap[player] == true) {
                afkMap.remove(player)
                player.sendMessage("${ChatColor.GRAY}You are no longer AFK.")

                TabAPI.getInstance().getPlayer(player.uniqueId)?.let { TabAPI.getInstance().tabListFormatManager?.setSuffix(it, null) }
                TabAPI.getInstance().getPlayer(player.uniqueId)?.let { TabAPI.getInstance().nameTagManager?.setSuffix(it, null) }
                TabAPI.getInstance().getPlayer(player.uniqueId)?.setTemporaryGroup(null)
            }
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, cmd: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Only players can do this!")
            return true
        }
        if (afkMap.contains(sender)) {
            setAFK(sender, false)
            return true
        } else {
            setAFK(sender, true)
        }
        return true;
    }

}