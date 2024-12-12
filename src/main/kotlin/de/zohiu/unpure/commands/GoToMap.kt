package de.zohiu.unpure.commands

import de.zohiu.unpure.UnPure
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class GoToMap : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, cmd: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            UnPure.maps.forEach { sender.sendMessage(it) }
            return true;
        }

        val world = Bukkit.getWorld(args[0]);
        if (world != null) {
            (sender as Player).teleport(world.spawnLocation)
        }
        return true;
    }
}