package de.zohiu.unpure.commands.worlds

import de.zohiu.unpure.UnPure
import de.zohiu.unpure.UnPure.Companion.templatesPath
import de.zohiu.unpure.chunkgenerator.VoidBiomeProvider
import de.zohiu.unpure.chunkgenerator.VoidChunkGenerator
import org.bukkit.Bukkit
import org.bukkit.WorldCreator
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import java.io.File

class CreateTemplate : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, cmd: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("unpure.admin")) {
            sender.sendMessage("You may not do that!")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("Please provide a name.")
            return true
        }

        val mapName = args[0];

        sender.sendMessage("CREATED ${UnPure.templatesPath}/$mapName");

        val worldCreator = WorldCreator("${UnPure.templatesPath}/$mapName");
        worldCreator.generator(VoidChunkGenerator())
        worldCreator.biomeProvider(VoidBiomeProvider())
        worldCreator.createWorld()
        val world = Bukkit.getWorld("${UnPure.templatesPath}/$mapName")!!
        world.save()
        UnPure.maps = File("maps/$templatesPath").listFiles()?.map { it.name }?.toTypedArray() ?: arrayOf()

        return true;
    }
}