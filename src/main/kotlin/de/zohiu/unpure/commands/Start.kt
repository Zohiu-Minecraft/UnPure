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
    var starting = false

    override fun onCommand(sender: CommandSender, command: Command, cmd: String, args: Array<out String>): Boolean {
        if (Bukkit.getOnlinePlayers().size < 2) {
            sender.sendMessage("There are not enough players online! (Min: 2)")
            return true
        }

        if (Game.openGames.size > 0) {
            if (sender !is Player) {
                sender.sendMessage("A game is already in progress.")
                return true
            }
            val game = Game.openGames[0]
            sender.sendTitle("A game is in progress", "You'll be able to play next round.", 5, 100, 5)
            sender.teleport(game.world.spawnLocation)
            sender.gameMode = GameMode.SPECTATOR
            return true
        }

        if (starting) {
            sender.sendMessage("A game is already starting.")
            return true
        }

        starting = true
        var i = 5
        var game: Game? = null

        UnPure.crimson.effectBuilder().repeat(5, 20) {
            Bukkit.broadcastMessage("Game starting in ${i}s.")
            i--
        }.run {
            if (Bukkit.getOnlinePlayers().size < 2) {
                sender.sendMessage("Start aborted! There are not enough players online! (Min: 2)")
                return@run
            }

            // Generate game ID
            val gameID = UUID.randomUUID().toString()

            // Initialize game
            game = Game(gameID, sender.name)

        }.wait(20).run {
            UnPure.lobby.players.forEach {
                game!!.players.add(it)
            }
            game!!.start()
            starting = false
        }.start()

        return true
    }
}