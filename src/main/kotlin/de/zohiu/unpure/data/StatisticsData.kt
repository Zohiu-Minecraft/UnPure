package de.zohiu.unpure.data

import de.zohiu.crimson.CacheLevel
import de.zohiu.unpure.UnPure
import de.zohiu.unpure.game.Game

class StatisticsData {
    companion object {
        // Write after game ends and when server stops or every 5 minutes if no game is running
        val database = UnPure.crimson.getDatabase("players",
            CacheLevel.WRITE_PERIODIC, 1000, 20 * 60 * 5
        ) { Game.openGames.size == 0 }

        fun commitData() {
            database.asyncCommitCache()
        }

        // Ingame stats
        val tableHumanWins = database.getTable("human_wins")
        val tableInfectedWins = database.getTable("infected_wins")
        val tableInfectedLosses = database.getTable("infected_losses")

        val tableKills = database.getTable("kills")
        val tableDeaths = database.getTable("deaths")
        val tableDamageDealt = database.getTable("damage_dealt")
        val tableDamageTaken = database.getTable("damage_taken")

        val tableBlocksMined = database.getTable("blocks_mined")
        val tableBlocksPlaced = database.getTable("blocks_placed")
        val tableOresMined = database.getTable("ores_mined")
        val tableJumps = database.getTable("jumps")

        // General stats
        val tableJoins = database.getTable("joins")
        val tableCratesOpened = database.getTable("crates_opened")
        val tablePlaytime = database.getTable("playtime")
        val tableFirstJoin= database.getTable("first_join")
    }
}