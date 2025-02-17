package de.zohiu.unpure.data

import de.zohiu.unpure.UnPure
import de.zohiu.unpure.game.Game
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


class Placeholders : PlaceholderExpansion() {
    override fun getIdentifier(): String {
        return "unpure"
    }

    override fun getAuthor(): String {
        return UnPure.instance.description.authors.joinToString(", ")
    }

    override fun getVersion(): String {
        return UnPure.instance.description.version
    }

    fun toTimestamp(millis: Long): String {
        // Convert milliseconds to Instant (UTC)
        val instant = Instant.ofEpochMilli(millis)

        // Format to a human-readable UTC date and time
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())

        // Print the formatted UTC date and time
        return formatter.format(instant)
    }

    fun formatPlaytime(playtimeMillis: Long): String {
        // Calculate hours, minutes, and seconds
        val hours = (playtimeMillis / 1000) / 3600
        val minutes = (playtimeMillis / 1000 % 3600) / 60
        val seconds = (playtimeMillis / 1000 % 3600) % 60

        // Return formatted string in HH:mm:ss format
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    // Placeholder is %unpure_param% for player
    override fun onPlaceholderRequest(player: Player?, param: String): String {
        if (player == null) return ""
        return when(param) {
            "human_wins" -> { DataOperations.getTableLong(player, StatisticsData.tableHumanWins) }
            "infected_wins" -> { DataOperations.getTableLong(player, StatisticsData.tableInfectedWins) }
            "infected_losses" -> { DataOperations.getTableLong(player, StatisticsData.tableInfectedLosses) }
            "kills" -> { DataOperations.getTableLong(player, StatisticsData.tableKills) }
            "deaths" -> { DataOperations.getTableLong(player, StatisticsData.tableDeaths) }
            "damage_dealt" -> { DataOperations.getTableLong(player, StatisticsData.tableDamageDealt) }
            "damage_taken" -> { DataOperations.getTableLong(player, StatisticsData.tableDamageTaken) }

            "blocks_mined" -> { DataOperations.getTableLong(player, StatisticsData.tableBlocksMined) }
            "blocks_placed" -> { DataOperations.getTableLong(player, StatisticsData.tableBlocksPlaced) }
            "ores_mined" -> { DataOperations.getTableLong(player, StatisticsData.tableOresMined) }
            "jumps" -> { DataOperations.getTableLong(player, StatisticsData.tableJumps) }

            "joins" -> { DataOperations.getTableLong(player, StatisticsData.tableJoins) }
            "crates_opened" -> { DataOperations.getTableLong(player, StatisticsData.tableCratesOpened) }
            "playtime" -> {
                if (!UnPure.playtimeCounter.contains(player)) 0
                else formatPlaytime(DataOperations.getTableLong(player, StatisticsData.tablePlaytime)
                        + System.currentTimeMillis() - UnPure.playtimeCounter[player]!!)
            }
            "first_join" -> {
                toTimestamp(DataOperations.getTableLong(player, StatisticsData.tableFirstJoin) + player.playerTimeOffset)
            }
            "coins" -> { DataOperations.getTableLong(player, CosmeticsData.tableCoins) }

            // ingame
            "ingame_role" -> {
                var role = ""
                for (index in Game.openGames.indices) {
                    val game = Game.openGames[index]
                    if (game.players.contains(player)) {
                        if (game.infected.contains(player)) role = "&2&lInfected"
                        else role = "&e&lHuman"
                        break
                    }
                }
                role
            }
            else -> { "" }
        }.toString()
    }
}