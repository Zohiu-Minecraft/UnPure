package de.zohiu.unpure.commands.worlds

import de.zohiu.unpure.UnPure
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class GoToMap : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, cmd: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("unpure.admin")) {
            sender.sendMessage("You may not do that!")
            return true
        }

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

class GoToMapTabComplete : TabCompleter {
    override fun onTabComplete(p0: CommandSender, p1: Command, p2: String, p3: Array<out String>): MutableList<String>? {
        val worlds = Bukkit.getWorlds()
        val worldarray: MutableList<String> = mutableListOf()
        for (world in worlds) {
            worldarray.add(world.name)
        }
        return worldarray
    }
}