package de.zohiu.unpure.game

import org.bukkit.Color
import org.bukkit.FireworkEffect
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Firework
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack


fun finalDeathEffect(player: Player, location: Location) {
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