package de.zohiu.unpure.commands

import de.zohiu.unpure.UnPure
import de.zohiu.unpure.chunkgenerator.VoidBiomeProvider
import de.zohiu.unpure.chunkgenerator.VoidChunkGenerator
import de.zohiu.unpure.game.Game
import org.bukkit.*
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.io.File
import java.util.*

class Start : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, cmd: String, args: Array<out String>): Boolean {
        // Generate game ID
        val gameID = UUID.randomUUID().toString()

        // Initialize game
        val game = Game(gameID, sender.name)
        UnPure.lobby.players.forEach {
            game.players.add(it)
        }

        game.start()
        return true
    }
}