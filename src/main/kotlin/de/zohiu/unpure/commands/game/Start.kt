package de.zohiu.unpure.commands.game

import de.zohiu.unpure.UnPure
import de.zohiu.unpure.commands.general.AFK
import de.zohiu.unpure.game.Game
import org.bukkit.*
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player


class Start : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, cmd: String, args: Array<out String>): Boolean {
        if (Bukkit.getOnlinePlayers().size < 2) {
            sender.sendMessage("There are not enough players online! (Min: 2)")
            return true
        }

        if (sender !is Player) {
            sender.sendMessage("Only players can do this!")
            return true
        }

        val availablePlayers: MutableList<Player> = mutableListOf()
        UnPure.lobby.players.forEach {
            if (!AFK.afkMap.contains(it) || it == sender) {
                availablePlayers.add(it)
            }
        }

        if (availablePlayers.size < 2) {
            sender.sendMessage("There need to be at least 2 non-AFK players online.")
            return true
        }

        for (game in Game.openGames) {
            if (game.players.contains(sender)) {
                sender.sendMessage("Your game is already starting.")
                return true
            }
        }

        val game = Game.createGame(sender)
        availablePlayers.forEach {
            it.sendMessage("You have been added to ${sender.name}'s game.")
            game.players.add(it)
        }
        game.start()

        return true
    }
}