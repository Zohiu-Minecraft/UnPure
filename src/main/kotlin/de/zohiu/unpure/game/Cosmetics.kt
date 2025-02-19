package de.zohiu.unpure.game

import de.zohiu.unpure.data.CosmeticsData
import org.bukkit.*
import org.bukkit.entity.EntityType
import org.bukkit.entity.Firework
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import java.io.Serializable


interface CosmeticType : Serializable {
    val type: String
    val name: String
    val rarity: Rarity
    val description: String
}

enum class Rarity(val percentage: Double) {
    DEFAULT(0.0), COMMON(0.6), RARE(0.2), EPIC(0.15), LEGENDARY(0.05);
}

enum class DeathEffect(override val rarity: Rarity, override val description: String) : CosmeticType {
    DEFAULT(Rarity.DEFAULT, "No special effects."),
    STAR(Rarity.COMMON, "Purple and black star-shaped firework.");

    override val type: String = this::class.simpleName!!
}

enum class WinEffect(override val rarity: Rarity, override val description: String) : CosmeticType {
    DEFAULT(Rarity.DEFAULT, "No special effects."),
    STAR(Rarity.COMMON, "Purple and black star-shaped firework.");

    override val type: String = this::class.simpleName!!
}

enum class InfectedOutfit(override val rarity: Rarity, override val description: String) : CosmeticType {
    DEFAULT(Rarity.DEFAULT, "Dead or alive?"),
    SKELETON(Rarity.COMMON, "Forgot their bow at home."),
    WITHER_SKELETON(Rarity.COMMON, "Tall and spooky."),
    CREEPER(Rarity.COMMON, "Ready to explode your future!"),

    TOAD(Rarity.RARE, "Please don't sue us, Nintendo ;-;"),
    VILLAGER(Rarity.RARE, "Is only here for the free emeralds."),
    TURTLE(Rarity.RARE, "Too cute to die <3"),

    GREENPIGISBACON(Rarity.EPIC, "&kOwns this game."),
    CLOWN(Rarity.EPIC, "The humans will die of laughing."),
    ZOHIU(Rarity.EPIC, "<Unfair advantage>"),

    PIGLIN(Rarity.LEGENDARY, "Ran out of gold. Now deals in human organs."),
    RACER(Rarity.LEGENDARY, "I.. am.. speed!"),
    ENDER_DRAGON(Rarity.LEGENDARY, "Has fly hacks (not really).");

    override val type: String = this::class.simpleName!!
}

enum class Trail(override val rarity: Rarity, override val description: String) : CosmeticType {
    DEFAULT(Rarity.DEFAULT, "No trail."),
    FIRE(Rarity.COMMON, "That's hot!");

    override val type: String = this::class.simpleName!!
}

enum class KillMessage(override val rarity: Rarity, override val description: String) : CosmeticType {
    DEFAULT(Rarity.DEFAULT, "&a%victim%&7 has been killed by &a%attacker%&7!"),
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

    override val type: String = this::class.simpleName!!
}


class Cosmetics {
    companion object {
        fun winEffect(player: Player) {
            val location = player.location
            var equipped = CosmeticsData.getEquippedCosmetic(player, WinEffect::class.simpleName!!, WinEffect.DEFAULT)

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
            var equipped = CosmeticsData.getEquippedCosmetic(player, DeathEffect::class.simpleName!!, DeathEffect.DEFAULT)

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
            val equipped = CosmeticsData.getEquippedCosmetic(player, InfectedOutfit::class.simpleName!!, InfectedOutfit.DEFAULT)

            when (equipped as InfectedOutfit) {
                InfectedOutfit.DEFAULT -> {
                    player.inventory.setItem(EquipmentSlot.FEET, ItemStack(Material.LEATHER_BOOTS))
                    player.inventory.setItem(EquipmentSlot.LEGS, ItemStack(Material.LEATHER_LEGGINGS))
                    player.inventory.setItem(EquipmentSlot.CHEST, ItemStack(Material.LEATHER_CHESTPLATE))
                    player.inventory.setItem(EquipmentSlot.HEAD, ItemStack(Material.ZOMBIE_HEAD))
                }

                else -> {

                }
            }
        }

        fun trail(player: Player) {
            var equipped = CosmeticsData.getEquippedCosmetic(player, Trail::class.simpleName!!, Trail.DEFAULT)
        }

        fun killMessage(attacker: Player, victim: Player): String {
            val equipped: CosmeticType = CosmeticsData.getEquippedCosmetic(attacker, KillMessage::class.simpleName!!, KillMessage.DEFAULT)
            val msg = (equipped as KillMessage).description
                    .replace("%victim%", victim.name).replace("&a%attacker%&7", attacker.name)
            return ChatColor.translateAlternateColorCodes('&', msg)
        }
    }
}