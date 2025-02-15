package de.zohiu.unpure.game

import de.zohiu.crimson.Effect
import de.zohiu.unpure.UnPure
import de.zohiu.unpure.chunkgenerator.VoidBiomeProvider
import de.zohiu.unpure.chunkgenerator.VoidChunkGenerator
import de.zohiu.unpure.config.Config
import de.zohiu.unpure.events.GameEvents
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.*
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.io.File
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.TimeUnit


class Game (private val gameID: String, val gameCreator: String) {
    companion object {
        @JvmStatic var openGames: MutableList<Game> = mutableListOf()
    }

    val timerTickDelay = 5
    val graceTimeSeconds = 60
    val gameTimeSeconds = 300
    val lastMinuteEffectsAtSecond = 30
    var elapsedTimeSeconds = 0
    var elapsedTimeTicks = 0

    var inProgress = false;

    var bossbar: BossBar;
    var eventListener: GameEvents
    lateinit var world: World
    lateinit var gameTimer: Effect;

    var players: MutableList<Player> = mutableListOf()
    var humans: MutableList<Player> = mutableListOf()
    var infected: MutableList<Player> = mutableListOf()

    var targetGamePath: String

    init {
        // Get random map
        val mapName: String = UnPure.maps.random()
        val mapPath = "${UnPure.mapsRoot}/${UnPure.templatesPath}/$mapName"
        targetGamePath = "${UnPure.gamesPath}/$gameID"

        // Clone the map
        File(mapPath).copyRecursively(File("${UnPure.mapsRoot}/$targetGamePath"), overwrite = true)

        // Bossbar
        bossbar = Bukkit.createBossBar(this.gameID, BarColor.GREEN, BarStyle.SOLID)
        bossbar.setTitle(Config.str("game.bossbar.default"))

        eventListener = GameEvents(this)
        openGames.add(this)
    }

    fun broadcast(message: String, targets: MutableList<Player>? = null) {
        if (targets != null) {
            targets.forEach { it.sendMessage("${Config.gameMessagePrefix}$message") }
        } else {
            players.forEach { it.sendMessage("${Config.gameMessagePrefix}$message") }
        }
    }

    fun title(title: String, subtitle: String, targets: MutableList<Player>? = null) {
        if (targets != null) {
            targets.forEach { it.sendTitle(title, subtitle, 5, 100, 5) }
        } else {
            players.forEach { it.sendTitle(title, subtitle, 5, 100, 5) }
        }
    }

    fun actionbar(message: String, targets: MutableList<Player>? = null) {
        if (targets != null) {
            targets.forEach { it.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent(message)) }
        } else {
            players.forEach { it.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent(message)) }
        }
    }

    fun start() {
        // Load the map
        val worldCreator = WorldCreator(targetGamePath);
        worldCreator.generator(VoidChunkGenerator())
        worldCreator.biomeProvider(VoidBiomeProvider())
        worldCreator.createWorld()

        // Initialize the map
        world = Bukkit.getWorld(targetGamePath)!!
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false)
        world.setGameRule(GameRule.KEEP_INVENTORY, true)
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false)
        world.setGameRule(GameRule.DISABLE_RAIDS, true)
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false)
        world.setGameRule(GameRule.DO_ENTITY_DROPS, false)
        world.setGameRule(GameRule.DO_FIRE_TICK, false)
        world.setGameRule(GameRule.DO_INSOMNIA, false)
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true)
        world.setGameRule(GameRule.DO_MOB_LOOT, false)
        world.setGameRule(GameRule.DO_PATROL_SPAWNING, false)
        world.setGameRule(GameRule.DO_TRADER_SPAWNING, false)
        world.setGameRule(GameRule.DO_VINES_SPREAD, false)
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false)
        world.setGameRule(GameRule.DO_WARDEN_SPAWNING, false)
        world.setGameRule(GameRule.MOB_GRIEFING, false)
        world.setGameRule(GameRule.PLAYERS_SLEEPING_PERCENTAGE, 101)
        world.setGameRule(GameRule.RANDOM_TICK_SPEED, 0)
        world.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false)
        world.setGameRule(GameRule.SPAWN_RADIUS, 0)
        world.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false)

        // Copy players to humans and select first infected
        humans = players.toMutableList()
        humans.shuffle()
        infected.add(humans.removeLast())

        players.forEach { bossbar.addPlayer(it) }
        humans.forEach { initHumanState(it, startInSpectator = true) }
        infected.forEach { initInfectedState(it, startInSpectator = true) }

        title(Config.str("game.title.infected"), Config.str("game.subtitle.infected"), infected)
        title(Config.str("game.title.human"), Config.str("game.subtitle.human"), humans)

        broadcast(Config.str("game.status.starting-in", arrayOf("5")))

        // Wait a bit before starting the game
        UnPure.crimson.effectBuilder().wait(100).run {
            inProgress = true
            UnPure.instance.server.pluginManager.registerEvents(eventListener, UnPure.instance)

            broadcast(Config.str("game.status.started"))

            bossbar.color = BarColor.GREEN
            humans.forEach {
                it.teleport(world.spawnLocation)
                it.gameMode = GameMode.SURVIVAL
            }
            infected.forEach {
                val loc = UnPure.waitingarea.spawnLocation
                loc.add(0.5, 0.0, 0.5)
                loc.yaw = -90.0f
                it.teleport(loc)
                it.gameMode = GameMode.ADVENTURE
                it.inventory.clear()
                it.removePotionEffect(PotionEffectType.GLOWING)
            }
            startGracePeriod()
        }.start()
    }

    fun stop() {
        if (!inProgress) { return }
        heartbeatTask?.abort()
        inProgress = false
        players.forEach {
            bossbar.removePlayer(it)
            resetPlayer(it, skipTeleport = true)
        }
        bossbar.removeAll()
        gameTimer.abort()
        HandlerList.unregisterAll(eventListener)
        openGames.remove(this)
        GameData.database.commitCache()

        // Wait a bit before ending the game
        UnPure.crimson.effectBuilder().wait(100).run {
            UnPure.waitingarea.players.forEach {
                resetPlayer(it, skipTeleport = true)
                val loc = UnPure.lobby.spawnLocation
                loc.add(0.5, 0.0, 0.5)
                loc.yaw = -90.0f
                it.teleport(loc)
                it.inventory.clear()
                it.gameMode = GameMode.ADVENTURE
            }
            world.players.forEach {
                resetPlayer(it, skipTeleport = true)
                val loc = UnPure.lobby.spawnLocation
                loc.add(0.5, 0.0, 0.5)
                loc.yaw = -90.0f
                it.teleport(loc)
                it.inventory.clear()
                it.gameMode = GameMode.ADVENTURE
            }
            Bukkit.unloadWorld(world, false)
            File("${UnPure.mapsRoot}/$targetGamePath").deleteRecursively()
        }.start()
    }

    fun infectPlayer(player: Player, killer: Player? = null) {
        if (infected.contains(player)) {
            broadcast(Config.str("game.player.died", arrayOf(player.name)))
            player.teleport(world.spawnLocation)
            GameData.addOneToTable(player, GameData.tableDeaths)
            return
        }
        if (!humans.contains(player)) { return }  // Just to be safe.
        GameData.addOneToTable(player, GameData.tableDeaths)

        if (killer != null) {
            broadcast(Config.str("game.player.infected", arrayOf(player.name, killer.name)))
            GameData.addOneToTable(killer, GameData.tableKills)
        } else {
            broadcast(Config.str("game.player.self-infected", arrayOf(player.name)))
        }

        humans.remove(player)
        infected.add(player)

        // Game ends when no humans left
        if (humans.isEmpty()) {
            player.gameMode = GameMode.SPECTATOR
            if (killer is Player) Cosmetics.winEffect(killer, player.location)
            Cosmetics.deathEffect(player, player.location)
            broadcast(Config.str("game.status.end.humans-dead"))
            infected.forEach { GameData.addOneToTable(it, GameData.tableInfectedWins) }
            stop()
        } else {
            initInfectedState(player)
        }
    }

    fun removePlayer(player: Player) {
        if (players.contains(player)) { players.remove(player) }

        if (humans.contains(player)) {
            humans.remove(player)
            if (humans.isEmpty()) {
                broadcast(Config.str("game.status.end.last-human-left"))
                stop()
            }
        }
        if (infected.contains(player)) {
            infected.remove(player)
            if (infected.isEmpty()) {
                broadcast(Config.str("game.status.end.last-infected-left"))
                stop()
            }
        }

    }

    private fun startGracePeriod() {
        gameTimer = UnPure.crimson.effectBuilder().repeatUntil({ elapsedTimeTicks > graceTimeSeconds * 20 }, timerTickDelay) {
            val remaining = graceTimeSeconds - elapsedTimeSeconds
            bossbar.setTitle(Config.str("game.bossbar.grace", arrayOf(remaining.toString())))
            bossbar.progress = Math.max(0.0, 1 - elapsedTimeTicks / (graceTimeSeconds * 20.0))
            gameTick()
        }.run {
            infected.forEach { initInfectedState(it, startInSpectator = false) }
            infected.forEach {
                it.teleport(world.spawnLocation)
                it.gameMode = GameMode.SURVIVAL
            }
            broadcast(Config.str("game.status.grace-over"))
            bossbar.color = BarColor.YELLOW
            startGamePeriod()
        }.start()
    }

    private fun startGamePeriod() {
        var humsent = false
        gameTimer.abort()  // Make sure that the grace period is actually stopped for sure
        gameTimer = UnPure.crimson.effectBuilder().repeatUntil({ elapsedTimeTicks > (gameTimeSeconds * 20) + (graceTimeSeconds * 20) }, timerTickDelay) {
            val remaining = gameTimeSeconds - elapsedTimeSeconds + graceTimeSeconds
            if (remaining == lastMinuteEffectsAtSecond) {
                bossbar.color = BarColor.RED
                if (!humsent) {
                    humans.forEach {
                        it.addPotionEffect(PotionEffect(PotionEffectType.GLOWING, PotionEffect.INFINITE_DURATION, 0))
                        broadcast(Config.str("game.status.last-minute"))
                    }
                }
                humsent = true
            }
            bossbar.setTitle(Config.str("game.bossbar.game-progress", arrayOf(remaining.toString())))
            bossbar.progress = 1 - (elapsedTimeTicks - (graceTimeSeconds * 20.0)) / (gameTimeSeconds * 20.0)

            gameTick()
        }.run {
            broadcast(Config.str("game.status.end.timeout"))
            humans.forEach { GameData.addOneToTable(it, GameData.tableHumanWins) }
            infected.forEach { GameData.addOneToTable(it, GameData.tableInfectedLosses) }
            stop()
        }.start()

        heartbeatTask = startHeartbeatTask()
    }

    // Every timer tick
    private fun gameTick() {
        actionbar(Config.str("game.actionbar.infected"), infected)
        actionbar(Config.str("game.actionbar.human"), humans)

        elapsedTimeTicks += timerTickDelay
        elapsedTimeSeconds = elapsedTimeTicks / 20
        infected.forEach { Cosmetics.trail(it) }
    }

    private fun resetPlayer(player: Player, skipTeleport: Boolean = false) {
        player.inventory.clear()
        player.health = 20.0
        player.saturation = 20.0f
        player.foodLevel = 20
        player.exp = 0.0f
        player.removePotionEffect(PotionEffectType.GLOWING)
        player.removePotionEffect(PotionEffectType.ABSORPTION)
        if (!skipTeleport) { player.teleport(world.spawnLocation) }
    }

    private fun initHumanState(player: Player, startInSpectator: Boolean = false) {
        resetPlayer(player)
        if (startInSpectator) { player.gameMode = GameMode.SPECTATOR }
        else { player.gameMode = GameMode.SURVIVAL }
        player.inventory.addItem(ItemStack(Material.WOODEN_SWORD))
        player.inventory.addItem(ItemStack(Material.WOODEN_PICKAXE))
        player.inventory.addItem(ItemStack(Material.WOODEN_AXE))
        player.inventory.addItem(ItemStack(Material.WOODEN_SHOVEL))
        player.inventory.addItem(ItemStack(Material.GOLDEN_APPLE))
    }

    private fun initInfectedState(player: Player, startInSpectator: Boolean = false) {
        resetPlayer(player)
        player.addPotionEffect(PotionEffect(PotionEffectType.GLOWING, PotionEffect.INFINITE_DURATION, 0))
        player.inventory.addItem(ItemStack(Material.WOODEN_AXE))
        player.inventory.addItem(ItemStack(Material.STONE_PICKAXE))
        Cosmetics.infectedOutfit(player)
        if (startInSpectator) { player.gameMode = GameMode.SPECTATOR }
    }

    private var heartbeatTask: Effect? = null
    private fun startHeartbeatTask(): Effect {
        return UnPure.crimson.effectBuilder().repeatForever(20) {
            // Check distance to all infected
            for (human in humans) {
                val max_distance = 16.0
                var current_distance = max_distance
                for (infected in infected) {
                    val distance = human.location.distance(infected.location)
                    if (distance < current_distance) {
                        current_distance = distance
                    }
                }

                if (current_distance < max_distance) {
                    val wb_size = 256

                    val distance_percent = 1 - (current_distance / max_distance)
                    val beats_per_second = 1 + (2 * distance_percent)

                    val tick_delay_between_beats = (20 / beats_per_second).toLong()

                    val tick_beat_speed = 2 + (6 * (1 - distance_percent)).toLong()

                    val tint: WorldBorder = UnPure.instance.getServer().createWorldBorder()
                    tint.center = human.location
                    tint.size = wb_size.toDouble()
                    tint.warningDistance = (wb_size / 2) + ((distance_percent * (wb_size / 2)).toInt()) + 1
                    human.worldBorder = tint
                    human.playSound(human, "minecraft:block.note_block.basedrum", SoundCategory.MASTER, 1f, 0.7f)

                    for (i in 0..<Math.round(beats_per_second)) {
                        Bukkit.getScheduler().runTaskLater(UnPure.instance, Runnable {
                            val tint1: WorldBorder = UnPure.instance.getServer().createWorldBorder()
                            tint1.center = human.location
                            tint1.size = wb_size.toDouble()
                            tint1.warningDistance = (wb_size / 2) + ((distance_percent * (wb_size / 2)).toInt()) + 1
                            human.worldBorder = tint1
                            human.playSound(
                                human,
                                "minecraft:block.note_block.basedrum",
                                SoundCategory.MASTER,
                                1f,
                                0.7f
                            )
                            Bukkit.getScheduler().runTaskLater(UnPure.instance, Runnable {
                                tint1.warningDistance = 0
                                human.worldBorder = tint1
                                human.playSound(
                                    human,
                                    "minecraft:block.note_block.basedrum",
                                    SoundCategory.MASTER,
                                    1f,
                                    1f
                                )
                            }, tick_beat_speed)
                        }, tick_delay_between_beats * i)
                    }
                } else {
                    val notint: WorldBorder = UnPure.instance.getServer().createWorldBorder()
                    notint.center = world.spawnLocation
                    notint.size = 1024.0
                    notint.warningDistance = 0

                    human.worldBorder = notint
                }
            }
        }
    }
}