package de.zohiu.unpure.data

import de.zohiu.crimson.CacheLevel
import de.zohiu.crimson.Table
import de.zohiu.unpure.UnPure
import de.zohiu.unpure.game.CosmeticType
import org.bukkit.entity.Player

class CosmeticsData {
    companion object {
        val database = UnPure.crimson.getDatabase("cosmetics", CacheLevel.GET, 500)  // Instant write

        val tableCoins = database.getTable("coins")

        val tableDeathEffectEquipped = database.getTable("death_effect_equipped")
        val tableDeathEffects = database.getTable("death_effects")

        val tableWinEffectEquipped = database.getTable("win_effect_equipped")
        val tableWinEffects = database.getTable("win_effects")

        val tableOutfitsEquipped = database.getTable("outfits_equipped")
        val tableOutfits = database.getTable("outfits")

        val tableTrailEquipped = database.getTable("trail_equipped")
        val tableTrails = database.getTable("trails")

        val tableKillMessageEquipped = database.getTable("kill_message_equipped")
        val tableKillMessages = database.getTable("kill_messages")


        fun unlockCosmetic(player: Player, cosmetic: CosmeticType) {
            var list = cosmetic.unlockedTable.get(player.uniqueId.toString())
            if (list == null) {
                list = mutableListOf(cosmetic)
                cosmetic.unlockedTable.set(player.uniqueId.toString(), list)
                return
            }
            (list as MutableList<CosmeticType>).add(cosmetic)
            cosmetic.unlockedTable.set(player.uniqueId.toString(), list)
        }

        fun getCosmetics(player: Player, table: Table): MutableList<CosmeticType> {
            var list: Any? = table.get(player.uniqueId.toString()) ?: return mutableListOf()
            return list as MutableList<CosmeticType>
        }

        fun getAllCosmetics(player: Player): MutableList<CosmeticType> {
            var list: MutableList<CosmeticType> = mutableListOf()
            listOf(tableDeathEffects, tableWinEffects, tableOutfits, tableTrails, tableKillMessages).forEach {
                val value = it.get(player.uniqueId.toString()) ?: return@forEach
                list += value as MutableList<CosmeticType>
            }
            return list
        }

        fun equipCosmetic(player: Player, cosmetic: CosmeticType) {
            cosmetic.equippedTable.set(player.uniqueId.toString(), cosmetic)
        }

        fun getEquippedCosmetic(player: Player, table: Table): CosmeticType? {
            val cosmetic = table.get(player.uniqueId.toString()) ?: return null
            return cosmetic as CosmeticType
        }
    }
}