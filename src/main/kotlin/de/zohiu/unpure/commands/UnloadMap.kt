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

class UnloadMap : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, cmd: String, args: Array<out String>): Boolean {
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
        };

        return true;
    }
}