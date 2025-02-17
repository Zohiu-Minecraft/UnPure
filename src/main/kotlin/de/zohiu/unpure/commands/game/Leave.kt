package de.zohiu.unpure.commands.game

import de.zohiu.unpure.game.Game
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class Leave : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, cmd: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Only players can do this!")
            return true
        }

        for (game in Game.openGames) {
            if (game.players.contains(sender)) {
                game.removePlayer(sender)
                sender.sendMessage("You have left ${game.host.name}'s game.")
                return true
            }

            if (game.spectators.contains(sender)) {
                game.disableSpectate(sender)
                sender.sendMessage("You stopped spectating ${game.host.name}'s game.")
                return true
            }
        }

        sender.sendMessage("You are not in a game.")
        return true
    }
}