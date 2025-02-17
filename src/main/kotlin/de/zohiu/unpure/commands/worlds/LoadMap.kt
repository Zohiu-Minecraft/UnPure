package de.zohiu.unpure.commands.worlds

import de.zohiu.unpure.UnPure
import de.zohiu.unpure.chunkgenerator.VoidBiomeProvider
import de.zohiu.unpure.chunkgenerator.VoidChunkGenerator
import org.bukkit.WorldCreator
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class LoadMap : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, cmd: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("unpure.admin")) {
            sender.sendMessage("You may not do that!")
            return true
        }

        var mapName = "";

        if (args.isEmpty()) {
            UnPure.maps.forEach { sender.sendMessage(it) }
            return true;
        }

        run loop@{
            UnPure.maps.forEach {
                if (it.contains(args[0])) {
                    mapName = it;
                    return@loop;
                }
            }
        }

        if (mapName == "") {
            sender.sendMessage("Not a valid map.");
            return true;
        }

        sender.sendMessage("LOADED ${UnPure.templatesPath}/$mapName");

        val worldCreator = WorldCreator("${UnPure.templatesPath}/$mapName");
        worldCreator.generator(VoidChunkGenerator())
        worldCreator.biomeProvider(VoidBiomeProvider())
        worldCreator.createWorld()

        return true;
    }
}

class LoadMapTabComplete : TabCompleter {
    override fun onTabComplete(p0: CommandSender, p1: Command, p2: String, p3: Array<out String>): MutableList<String>? {
        val worlds = UnPure.maps
        val worldarray: MutableList<String> = mutableListOf()
        for (world in worlds) {
            worldarray.add(world)
        }
        return worldarray
    }
}