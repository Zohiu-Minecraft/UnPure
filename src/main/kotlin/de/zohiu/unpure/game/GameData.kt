package de.zohiu.unpure.game

import de.zohiu.crimson.CacheLevel
import de.zohiu.crimson.Table
import de.zohiu.unpure.UnPure
import org.bukkit.entity.Player

class GameData {
    companion object {
        val database = UnPure.crimson.getDatabase("players", CacheLevel.FULL)  // Write after game ends

        val tableHumanWins = database.getTable("human_wins")
        val tableInfectedWins = database.getTable("infected_wins")
        val tableInfectedLosses = database.getTable("infected_losses")

        val tableKills = database.getTable("kills")
        val tableDeaths = database.getTable("deaths")

        val tableBlocksMined = database.getTable("blocks_mined")

        fun addOneToTable(player: Player, table: Table) {
            var current = table.get(player.uniqueId.toString())
            if (current == null) current = 0
            table.set(player.uniqueId.toString(), current as Int + 1)
        }

        fun getTableVal(player: Player, table: Table): Int {
            var current = table.get(player.uniqueId.toString())
            if (current == null) current = 0
            return current as Int
        }
    }
}