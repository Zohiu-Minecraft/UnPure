package de.zohiu.unpure.commands

import de.zohiu.unpure.UnPure
import de.zohiu.unpure.chunkgenerator.VoidBiomeProvider
import de.zohiu.unpure.chunkgenerator.VoidChunkGenerator
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class LoadMap : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, cmd: String, args: Array<out String>): Boolean {
        var mapName: String = "";

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

        sender.sendMessage("${UnPure.templatesPath}/$mapName");

        val worldCreator = WorldCreator("${UnPure.templatesPath}/$mapName");
        worldCreator.generator(VoidChunkGenerator())
        worldCreator.biomeProvider(VoidBiomeProvider())
        worldCreator.createWorld()

        return true;
    }
}