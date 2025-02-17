package de.zohiu.unpure.config

import org.bukkit.ChatColor

class Messages {
    companion object {
        @JvmStatic val gameMessagePrefix = ChatColor.translateAlternateColorCodes('&',
            "&2&lUnPure &8» &r");

        @JvmStatic val messages = hashMapOf(
            "chat.format.general" to "&7<%0%> &r%1%",
            "chat.format.infected" to "&2&l<%0%> &r%1%",
            "chat.format.human" to "&e&l<%0%> &r%1%",

            "game.title.infection" to "&7You have been &2&linfected",
            "game.title.infected" to "&7You are &2&linfected",
            "game.subtitle.infected" to "&8Kill all humans.",

            "game.title.human" to "&7You are a &e&lhuman",
            "game.subtitle.human" to "&8Don't let the infected get you!",

            "game.actionbar.infected" to "&8Role: &2&lInfected",
            "game.actionbar.human" to "&8Role: &e&lHuman",

            "game.coin" to "&7You have recieved a &acoin &7 for winning!",

            "game.status.starting-in" to "&e&lHumans &r&7spawn in &a%0%s!",
            "game.status.started" to "&7The game has started!",
            "game.status.grace-over" to "&7The &2&linfected &r&7have spawned!",
            "game.status.last-minute" to "&a&lTime almost over! All humans have been revealed!",

            "game.status.end.timeout" to "&lTime is up! &e&lHumans &7win!",
            "game.status.end.humans-dead" to "&7There are no humans left. &2&lInfected &7win!",
            "game.status.end.last-human-left" to "&7The last human has quit. &2&lInfected &7win!",
            "game.status.end.last-infected-left" to "&7The last infected has quit. &e&lHumans &7win!",
            "game.status.player-left" to "&a%0% &7has left the game.",

            "game.player.died" to "&a%0% &7has died.",
            "game.player.self-infected" to "&a%0% &7has infected themself!",

            "game.bossbar.default" to "&a&lStarting soon!",
            "game.bossbar.grace" to "&2&lGrace period &7(%0%s remaining)",
            "game.bossbar.game-progress" to "&e&l%0%s remaining",
        )

        @JvmStatic fun str(key: String, args: Array<String>? = null): String {
            var message = messages[key] ?: return key
            message = ChatColor.translateAlternateColorCodes('&', message)
            if (args == null) { return message }
            var index = 0
            args.forEach {
                message = message.replace("%$index%", it)
                index++
            }
            return message
        }
    }
}