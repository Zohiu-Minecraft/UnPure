package de.zohiu.unpure.game

import de.zohiu.crimson.Table
import de.zohiu.unpure.data.CosmeticsData
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Color
import org.bukkit.FireworkEffect
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Firework
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

interface CosmeticType {
    val name: String
    val rarity: Rarity
    val unlockedTable: Table
    val equippedTable: Table
}

enum class Rarity(val percentage: Double) {
    COMMON(0.6), RARE(0.2), EPIC(0.15), LEGENDARY(0.05);
}

enum class DeathEffect(override val rarity: Rarity) : CosmeticType {
    STAR(Rarity.COMMON);

    override val equippedTable: Table
        get() = CosmeticsData.tableDeathEffectEquipped
    override val unlockedTable: Table
        get() = CosmeticsData.tableDeathEffects
}

enum class WinEffect(override val rarity: Rarity) : CosmeticType {
    STAR(Rarity.COMMON);

    override val equippedTable: Table
        get() = CosmeticsData.tableWinEffectEquipped
    override val unlockedTable: Table
        get() = CosmeticsData.tableWinEffects
}

enum class InfectedOutfit(override val rarity: Rarity) : CosmeticType {
    SKELETON(Rarity.COMMON),

    VILLAGER(Rarity.RARE), TURTLE(Rarity.RARE),

    CLOWN(Rarity.EPIC),

    GREENPIGISBACON(Rarity.LEGENDARY), RACER(Rarity.LEGENDARY), ZOHIU(Rarity.LEGENDARY);

    override val equippedTable: Table
        get() = CosmeticsData.tableOutfitsEquipped
    override val unlockedTable: Table
        get() = CosmeticsData.tableOutfits
}

enum class Trail(override val rarity: Rarity) : CosmeticType {
    FIRE(Rarity.COMMON);

    override val equippedTable: Table
        get() = CosmeticsData.tableTrailEquipped
    override val unlockedTable: Table
        get() = CosmeticsData.tableTrails
}

enum class KillMessage(override val rarity: Rarity, val template: String) : CosmeticType {
    HUNGRY(Rarity.COMMON, "&a%victim%&7 had their brains devoured by &a%attacker%&7!"),
    RAGE_QUIT(Rarity.COMMON, "&a%victim%&7 was Alt-F4'ed by &a%attacker%&7!"),

    OVERTIME(Rarity.RARE, "&a%victim%&7 was living on borrowed time from &a%attacker%&7!"),
    HACKER(Rarity.RARE, "&a%victim%&7 was was doxxed by &a%attacker%&7!"),
    UNPURE(Rarity.RARE, "&a%victim%&7 became UnPure thanks to &a%attacker%&7!"),
    JOKER(Rarity.RARE, "&a%victim%&7 became the luaghing stock of the town due to &a%attacker%&7!"),

    BOOOOM(Rarity.EPIC, "&a%victim%&7 received 5 big booms by &a%attacker%&7!"),
    BAD_MILKMAN(Rarity.EPIC, "&a%victim%&7 was served spoiled milk by &a%attacker%&7!"),
    THUG(Rarity.EPIC, "&a%victim%&7 was jumped by &a%attacker%&7 in the car park!"),

    GTA_VI(Rarity.LEGENDARY, "&a%victim%&7 was killed by &a%attacker%&7 before the release of GTA VI!");

    override val equippedTable: Table
        get() = CosmeticsData.tableKillMessageEquipped
    override val unlockedTable: Table
        get() = CosmeticsData.tableKillMessages
}


class Cosmetics {
    companion object {
        fun winEffect(player: Player) {
            val location = player.location
            var equipped = CosmeticsData.getEquippedCosmetic(player, CosmeticsData.tableWinEffectEquipped)

            val builder = FireworkEffect.builder()
            builder.with(FireworkEffect.Type.STAR).withFlicker().withTrail()
                .withColor(Color.FUCHSIA).withColor(Color.WHITE).withColor(Color.BLACK)


            val fw = location.world!!.spawnEntity(location, EntityType.FIREWORK_ROCKET) as Firework
            val fwm = fw.fireworkMeta

            fwm.power = 1
            fwm.addEffect(builder.build())

            fw.fireworkMeta = fwm
            fw.detonate()

            val fw2 = location.world!!.spawnEntity(location, EntityType.FIREWORK_ROCKET) as Firework
            fw2.fireworkMeta = fwm
        }

        fun deathEffect(player: Player, location: Location) {
            var equipped = CosmeticsData.getEquippedCosmetic(player, CosmeticsData.tableDeathEffectEquipped)

            val builder = FireworkEffect.builder()
            builder.with(FireworkEffect.Type.STAR).withFlicker().withTrail()
                .withColor(Color.FUCHSIA).withColor(Color.WHITE).withColor(Color.BLACK)


            val fw = location.world!!.spawnEntity(location, EntityType.FIREWORK_ROCKET) as Firework
            val fwm = fw.fireworkMeta

            fwm.power = 1
            fwm.addEffect(builder.build())

            fw.fireworkMeta = fwm
            fw.detonate()

            val fw2 = location.world!!.spawnEntity(location, EntityType.FIREWORK_ROCKET) as Firework
            fw2.fireworkMeta = fwm
        }

        fun infectedOutfit(player: Player) {
            var equipped = CosmeticsData.getEquippedCosmetic(player, CosmeticsData.tableOutfitsEquipped)
            if (equipped == null) {
                player.inventory.setItem(EquipmentSlot.FEET, ItemStack(Material.LEATHER_BOOTS))
                player.inventory.setItem(EquipmentSlot.LEGS, ItemStack(Material.LEATHER_LEGGINGS))
                player.inventory.setItem(EquipmentSlot.CHEST, ItemStack(Material.LEATHER_CHESTPLATE))
                player.inventory.setItem(EquipmentSlot.HEAD, ItemStack(Material.ZOMBIE_HEAD))
                return
            }

            when (equipped as InfectedOutfit) {
                else -> {

                }
            }
        }

        fun trail(player: Player) {
            var equipped = CosmeticsData.getEquippedCosmetic(player, CosmeticsData.tableTrailEquipped)
        }

        fun killMessage(attacker: Player, victim: Player): String {
            val equipped: CosmeticType? = CosmeticsData.getEquippedCosmetic(attacker, CosmeticsData.tableKillMessageEquipped)
            val msg = if (equipped == null) {
                "&a%victim%&7 &7was killed by &a&a%attacker%&7"
                    .replace("%victim%", victim.name).replace("&a%attacker%&7", attacker.name)
            } else {
                (equipped as KillMessage).template
                    .replace("%victim%", victim.name).replace("&a%attacker%&7", attacker.name)
            }
            return ChatColor.translateAlternateColorCodes('&', msg)
        }
    }
}