package de.zohiu.unpure.data

import de.zohiu.crimson.CacheLevel
import de.zohiu.unpure.UnPure
import de.zohiu.unpure.game.*
import org.bukkit.entity.Player

class CosmeticsData {
    companion object {
        val database = UnPure.crimson.getDatabase("cosmetics", CacheLevel.GET, 500)  // Instant write

        val tableCoins = database.getTable("coins")

        val unlockedCosmetics = database.getTable("unlocked")
        val equippedCosmetics = database.getTable("equipped")


        fun unlockCosmetic(player: Player, cosmetic: CosmeticType) {
            unlockedCosmetics.set("${player.uniqueId}_${cosmetic::class.simpleName!!}_${cosmetic}", true)
        }

        fun getAllCosmetics() : MutableList<CosmeticType> {
            val allCosmetics: MutableList<CosmeticType> = mutableListOf()
            allCosmetics.addAll(DeathEffect.values())
            allCosmetics.addAll(WinEffect.values())
            allCosmetics.addAll(InfectedOutfit.values())
            allCosmetics.addAll(Trail.values())
            allCosmetics.addAll(KillMessage.values())
            return allCosmetics
        }

        fun getUnlockedCosmetics(player: Player, cosmetics: List<CosmeticType>): MutableList<CosmeticType> {
            val allCosmetics: MutableList<CosmeticType> = cosmetics.toMutableList()
            allCosmetics.removeIf {
                unlockedCosmetics.get("${player.uniqueId}_${it::class.simpleName!!}_${it}") == null && it.name != "DEFAULT"
            }
            return allCosmetics
        }

        fun getAllUnlockedCosmetics(player: Player): MutableList<CosmeticType> {
            return getUnlockedCosmetics(player, getAllCosmetics())
        }

        fun equipCosmetic(player: Player, cosmetic: CosmeticType) {
            equippedCosmetics.set("${player.uniqueId}_${cosmetic.type}", cosmetic)
        }

        fun getEquippedCosmetic(player: Player, cosmeticType: String, default: CosmeticType): CosmeticType {
            return (equippedCosmetics.get("${player.uniqueId}_${cosmeticType}") as CosmeticType?) ?: default
        }
    }
}