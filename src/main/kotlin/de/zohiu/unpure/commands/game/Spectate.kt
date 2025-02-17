package de.zohiu.unpure.commands.game

import de.zohiu.unpure.game.Game
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class Spectate : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, cmd: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Only players can do this!")
            return true
        }

        for (game in Game.openGames) {
            if (game.players.contains(sender)) {
                sender.sendMessage("You can't spectate while playing.")
                return true
            }
        }

        if (args.isEmpty()) {
            sender.sendMessage("Please specify a player to spectate.")
            return true
        }

        val player: Player? = Bukkit.getPlayer(args[0])
        if (player == null) {
            sender.sendMessage("That player is not online.")
            return true
        }

        for (game in Game.openGames) {
            if (game.players.contains(player)) {
                game.enableSpectate(sender, player)
                sender.sendMessage("You are now spectating ${player.name}.")
                return true
            }
        }

        sender.sendMessage("That player is not in a game.")
        return true
    }
}

// Needed to show hidden players (players in games)
class SpectateTabComplete: TabCompleter {
    override fun onTabComplete(p0: CommandSender, p1: Command, p2: String, p3: Array<out String>): MutableList<String>? {
        val playerarray: MutableList<String> = mutableListOf()
        for (player in Bukkit.getOnlinePlayers()) {
            playerarray.add(player.name)
        }
        return playerarray
    }
}