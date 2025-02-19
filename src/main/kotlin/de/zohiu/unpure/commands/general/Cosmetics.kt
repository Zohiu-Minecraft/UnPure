package de.zohiu.unpure.commands.general

import de.zohiu.unpure.gui.CosmeticsGUI
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class Cosmetics : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, cmd: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Only players can do this!")
            return true
        }
        CosmeticsGUI(sender).show()
        return true;
    }
}