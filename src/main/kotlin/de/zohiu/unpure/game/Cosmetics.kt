package de.zohiu.unpure.game

import de.zohiu.crimson.CacheLevel
import de.zohiu.unpure.UnPure
import org.bukkit.Color
import org.bukkit.FireworkEffect
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Firework
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

enum class Rarity {
    COMMON, RARE, EPIC, LEGENDARY
}

enum class DeathEffect(rarity: Rarity) {
    DEFAULT(Rarity.COMMON), STAR(Rarity.COMMON),
}

enum class WinEffect(rarity: Rarity) {
    DEFAULT(Rarity.COMMON), STAR(Rarity.COMMON),
}

enum class InfectedOutfit(rarity: Rarity) {
    DEFAULT(Rarity.COMMON), SKELETON(Rarity.COMMON),
}

enum class Trail(rarity: Rarity) {
    DEFAULT(Rarity.COMMON), FIRE(Rarity.COMMON),
}


class Cosmetics {
    companion object {
        val database = UnPure.crimson.getDatabase("players", CacheLevel.GET)  // Instant write

        fun winEffect(player: Player, location: Location) {
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
            player.inventory.setItem(EquipmentSlot.FEET, ItemStack(Material.LEATHER_BOOTS))
            player.inventory.setItem(EquipmentSlot.LEGS, ItemStack(Material.LEATHER_LEGGINGS))
            player.inventory.setItem(EquipmentSlot.CHEST, ItemStack(Material.LEATHER_CHESTPLATE))
            player.inventory.setItem(EquipmentSlot.HEAD, ItemStack(Material.ZOMBIE_HEAD))
        }

        fun trail(player: Player) {

        }
    }
}