package de.zohiu.unpure.commands.worlds

import de.zohiu.unpure.UnPure
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class UnloadMap : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, cmd: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("unpure.admin")) {
            sender.sendMessage("You may not do that!")
            return true
        }

        if (args.isEmpty()) {
            Bukkit.getWorlds().forEach {
                sender.sendMessage(it.toString());
            }
            return true;
        }

        val world = Bukkit.getWorld(args[0]);
        if (world != null) {
            world.players.forEach { it.teleport(UnPure.lobby.spawnLocation) }
            Bukkit.unloadWorld(world, true)
            sender.sendMessage("UNLOADED ${UnPure.templatesPath}/${world.name}")
            return true
        };

        sender.sendMessage("That world is not loaded.")
        return true;
    }
}

class UnloadMapTabComplete : TabCompleter {
    override fun onTabComplete(p0: CommandSender, p1: Command, p2: String, p3: Array<out String>): MutableList<String>? {
        val worlds = Bukkit.getWorlds()
        val worldarray: MutableList<String> = mutableListOf()
        for (world in worlds) {
            worldarray.add(world.name)
        }
        return worldarray
    }
}