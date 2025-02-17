package de.zohiu.unpure.lobby

import de.zohiu.unpure.UnPure
import de.zohiu.unpure.game.*
import de.zohiu.unpure.data.CosmeticsData
import de.zohiu.unpure.data.DataOperations
import de.zohiu.unpure.data.StatisticsData
import org.bukkit.*
import org.bukkit.block.data.BlockData
import org.bukkit.entity.EntityType
import org.bukkit.entity.Firework
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class Crate : Listener {
    var opening = false

    @EventHandler
    fun onCrateClick(event: PlayerInteractEvent) {
        if (event.player.world != UnPure.lobby) return
        if (event.clickedBlock == null) return
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        if (event.clickedBlock!!.type != Material.ENDER_CHEST) return
        event.isCancelled = true

        val coins = DataOperations.getTableInt(event.player, CosmeticsData.tableCoins)

        val possibilities = getPossibleCosmeticWins(event.player)
        if (possibilities.size == 0) {
            event.player.sendMessage("You already unlocked all available cosmetics.")
            return
        }

        if (opening) {
            event.player.sendMessage("The crate is already being opened.")
            return
        }
        opening = true
        val crateLoc = event.clickedBlock!!.location.add(0.5, 0.5, 0.5)
        val blockData = event.clickedBlock!!.blockData.clone()
        event.clickedBlock!!.type = Material.RESPAWN_ANCHOR
        val world = crateLoc.world!!

        var index = 1.0
        var intIndex = 0
        val blockBackup: MutableList<List<Any>> = mutableListOf()

        world.playSound(crateLoc, Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1f)
        world.playSound(crateLoc, Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 1f)
        UnPure.crimson.effectBuilder().repeat(70, 1) {
            // Spiral
            val spiralLoc = crateLoc.clone().add(sin(index)*(index*0.1), index*0.2, -cos(index)*(index*0.1))

            if (intIndex % 2 == 0) {
                world.playSound(spiralLoc, Sound.BLOCK_NOTE_BLOCK_CHIME, (index.toFloat() * 0.2f) - 0.2f, (index.toFloat() * 0.1f) - 1f)
            }

            // world.spawnParticle(Particle.FLAME, spiralLoc, 5, 0.0, 0.0, 0.0, 0.05) // Amount, offset, speed
            world.spawnParticle(Particle.END_ROD, spiralLoc, 2, 0.0, 0.0, 0.0, 0.05) // Amount, offset, speed
            index *= 1.05
            intIndex++

        }.run{ index = 0.1; intIndex = 2 }.repeat(6, 8) {
            // Random blobs
            val randLoc = crateLoc.clone().add(
                Random.nextDouble(-index*3, index*3),
                Random.nextDouble(0.0, 5.0),
                Random.nextDouble(-index*3, index*3)
            )
            world.spawnParticle(Particle.PORTAL, randLoc, 10, 0.0, 0.0, 0.0, 0.1)
            world.spawnParticle(Particle.TOTEM_OF_UNDYING, randLoc, 15, 0.0, 0.0, 0.0, index)
            world.spawnParticle(Particle.END_ROD, randLoc, 2, 0.0, 0.0, 0.0, 0.05)

            // Rising cloud
            world.spawnParticle(Particle.FLAME, crateLoc.clone().add(0.0, index*3, 0.0), 5, 0.0, 0.0, 0.0, 0.1)
            world.spawnParticle(Particle.END_ROD, crateLoc.clone().add(0.0, index*3, 0.0), 2, 0.0, 0.0, 0.0, 0.05) // Amount, offset, speed

            world.playSound(randLoc, Sound.BLOCK_DECORATED_POT_PLACE, 1f, 1f)
            world.playSound(crateLoc, Sound.UI_HUD_BUBBLE_POP, 1f, 1f)
            world.playSound(crateLoc, Sound.ENTITY_ENDER_EYE_DEATH, 1f, 1f)

            // Changing blocks in the floor
            val locBelow = crateLoc.clone().subtract(0.0, 2.0, 0.0)
            for (x in (locBelow.blockX - intIndex)..(locBelow.blockX + intIndex)) {
                for (z in (locBelow.blockZ - intIndex)..(locBelow.blockZ + intIndex)) {
                    val y = locBelow.blockY
                    val l = Location(world, x.toDouble() + 0.5, y.toDouble(), z.toDouble() + 0.5)
                    if (l.distance(crateLoc) > intIndex) {
                        continue
                    }

                    val block =  world.getBlockAt(x, y, z)
                    val loc = Location(world, x.toDouble(), y.toDouble(), z.toDouble())
                    var exists = false
                    blockBackup.forEach { if (it[0] == loc) exists = true }
                    if (!exists) {
                        blockBackup.add(listOf(
                            loc.clone(),
                            block.type,
                            block.blockData.clone()
                        ))
                    }
                    world.spawnParticle(Particle.SMOKE, loc.clone().add(0.0, 1.0, 0.0), 2, 0.0, 0.0, 0.0, 0.1) // Amount, offset, speed
                    if (Random.nextInt(0, 2) == 0) block.type = Material.POLISHED_BLACKSTONE_BRICKS
                    else if (Random.nextInt(0, 2) == 0) block.type = Material.POLISHED_BLACKSTONE
                    else if (Random.nextInt(0, 2) == 0) block.type = Material.CRACKED_POLISHED_BLACKSTONE_BRICKS
                    else block.type = Material.CHISELED_POLISHED_BLACKSTONE
                }
            }
            
            index += 0.1
            intIndex++
        }.run {
            // GIVE REWARD
            val rarity = giveCrateReward(event.player, possibilities)
            // Use rarity to change effect here.

            // Firework
            val builder = FireworkEffect.builder()
            builder.with(FireworkEffect.Type.BALL).withFlicker().withTrail()
                .withColor(Color.WHITE).withColor(Color.PURPLE).withColor(Color.TEAL)

            val fw = crateLoc.world!!.spawnEntity(crateLoc.clone().add(0.0, 2.0, 0.0),
                EntityType.FIREWORK_ROCKET) as Firework
            val fwm = fw.fireworkMeta

            fwm.power = 1
            fwm.addEffect(builder.build())

            fw.fireworkMeta = fwm
            fw.detonate()

            val fw2 = crateLoc.world!!.spawnEntity(crateLoc, EntityType.FIREWORK_ROCKET) as Firework
            fw2.fireworkMeta = fwm
            world.playSound(crateLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1f)

        }.wait(10).run {
            world.playSound(crateLoc, Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f)
            world.playSound(crateLoc, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f)
            world.playSound(crateLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 1f)
            world.spawnParticle(Particle.EXPLOSION, crateLoc, 1, 0.0, 0.0, 0.0, 0.5) // Amount, offset, speed
            event.clickedBlock!!.type = Material.ENDER_CHEST
            event.clickedBlock!!.blockData = blockData
            opening = false

            // Changing blocks back
            val removeeffect = UnPure.crimson.effectBuilder()
            blockBackup.forEach {
                removeeffect.run {
                    val location = it[0] as Location
                    val material = it[1] as Material
                    val data = it[2] as BlockData
                    world.getBlockAt(location).type = material
                    world.getBlockAt(location).blockData = data
                    world.spawnParticle(Particle.CLOUD, location.clone().add(0.0, 1.0, 0.0), 1, 0.0, 0.0, 0.0, 0.1) // Amount, offset, speed
                    world.playSound(location, Sound.BLOCK_STONE_BREAK, 1f, 1f)
                    world.playSound(location, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.2f, 1f)
                }
                if (Random.nextInt(0, 3) == 0) removeeffect.wait(1)
            }
            removeeffect.start()
        }.start()
    }

    private fun getPossibleCosmeticWins(player: Player): MutableList<CosmeticType> {
        val unlockedOutfits = CosmeticsData.getAllCosmetics(player)
        val possibilities = (
                DeathEffect.entries
                        + WinEffect.entries
                        + InfectedOutfit.entries
                        + Trail.entries
                        + KillMessage.entries
                ).toMutableList()
        possibilities.removeIf { unlockedOutfits.contains(it) }
        return possibilities as MutableList<CosmeticType>
    }

    private fun giveCrateReward(player: Player, possibilities: MutableList<CosmeticType>): Rarity {
        // Adjust for rarity
        val adjustedPossibilites: MutableList<CosmeticType> = mutableListOf()
        possibilities.forEach { cosmetic ->
            repeat((cosmetic.rarity.percentage * 100).toInt()) {
                adjustedPossibilites.add(cosmetic)
            }
        }

        // unlock chosen choice
        val choice = adjustedPossibilites.shuffled()[0]
        CosmeticsData.unlockCosmetic(player, choice)
        DataOperations.addOneToTable(player, StatisticsData.tableCratesOpened)
        player.sendMessage("You won a ${choice.javaClass}: ${choice.name}")
        return choice.rarity
    }
}