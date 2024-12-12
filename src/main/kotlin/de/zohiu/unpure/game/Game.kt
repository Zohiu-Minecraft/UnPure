package de.zohiu.unpure.game

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
import javax.swing.text.Position

class Game (val gameID: String, val gameCreator: String) {
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
    var world: World
    var eventListener: GameEvents
    lateinit var gameTimer: BukkitTask;

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

        bossbar = Bukkit.createBossBar(gameID, BarColor.GREEN, BarStyle.SOLID)
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
        gameTimer = object : BukkitRunnable() { override fun run() {
            inProgress = true
            UnPure.instance.server.pluginManager.registerEvents(eventListener, UnPure.instance)

            broadcast(Config.str("game.status.started"))

            bossbar.color = BarColor.GREEN
            humans.forEach {
                it.teleport(world.spawnLocation)
                it.gameMode = GameMode.SURVIVAL
            }
            startGracePeriod()
        }}.runTaskLater(UnPure.instance, 100)
    }

    fun stop() {
        if (!inProgress) { return }
        inProgress = false
        players.forEach {
            bossbar.removePlayer(it)
            resetPlayer(it, skipTeleport = true)
        }
        bossbar.removeAll()
        gameTimer.cancel()
        HandlerList.unregisterAll(eventListener)
        openGames.remove(this)

        // Wait a bit before ending the game
        gameTimer = object : BukkitRunnable() { override fun run() {
            world.players.forEach {
                resetPlayer(it, skipTeleport = true)
                it.teleport(UnPure.lobby.spawnLocation)
                it.gameMode = GameMode.ADVENTURE
            }
            Bukkit.unloadWorld(world, false)
            File("${UnPure.mapsRoot}/$targetGamePath").deleteRecursively()
        }}.runTaskLater(UnPure.instance, 100)
    }

    fun infectPlayer(player: Player, killer: Player? = null) {
        if (infected.contains(player)) {
            broadcast(Config.str("game.player.died", arrayOf(player.name)))
            player.teleport(world.spawnLocation)
            return
        }
        if (!humans.contains(player)) { return }  // Just to be safe.

        if (killer != null) {
            broadcast(Config.str("game.player.infected", arrayOf(player.name, killer.name)))
        } else {
            broadcast(Config.str("game.player.self-infected", arrayOf(player.name)))
        }

        humans.remove(player)
        infected.add(player)

        // Game ends when no humans left
        if (humans.isEmpty()) {
            player.gameMode = GameMode.SPECTATOR
            broadcast(Config.str("game.status.end.humans-dead"))
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
        gameTimer = object : BukkitRunnable() { override fun run() {
            val remaining = graceTimeSeconds - elapsedTimeSeconds
            bossbar.setTitle(Config.str("game.bossbar.grace", arrayOf(remaining.toString())))
            bossbar.progress = 1 - elapsedTimeTicks / (graceTimeSeconds * 20.0)

            gameTick()
            if (elapsedTimeTicks > graceTimeSeconds * 20) {
                infected.forEach {
                    it.teleport(world.spawnLocation)
                    it.gameMode = GameMode.SURVIVAL
                }
                broadcast(Config.str("game.status.grace-over"))
                bossbar.color = BarColor.YELLOW
                startGamePeriod()
                this.cancel()
            }
        }}.runTaskTimer(UnPure.instance, 0, timerTickDelay.toLong())
    }

    private fun startGamePeriod() {
        gameTimer = object : BukkitRunnable() { override fun run() {
            val remaining = gameTimeSeconds - elapsedTimeSeconds + graceTimeSeconds
            if (remaining == lastMinuteEffectsAtSecond) {
                bossbar.color = BarColor.RED
                humans.forEach {
                    it.addPotionEffect(PotionEffect(PotionEffectType.GLOWING, PotionEffect.INFINITE_DURATION, 0))
                    broadcast(Config.str("game.status.last-minute"))
                }
            }
            bossbar.setTitle(Config.str("game.bossbar.game-progress", arrayOf(remaining.toString())))
            bossbar.progress = 1 - (elapsedTimeTicks - (graceTimeSeconds * 20.0)) / (gameTimeSeconds * 20.0)

            gameTick()
            if (elapsedTimeTicks > (gameTimeSeconds * 20) + (graceTimeSeconds * 20)) {
                broadcast(Config.str("game.status.end.timeout"))
                stop()
                this.cancel()
            }
        }}.runTaskTimer(UnPure.instance, 0, timerTickDelay.toLong())
    }

    // Every timer tick
    private fun gameTick() {
        actionbar(Config.str("game.actionbar.infected"), infected)
        actionbar(Config.str("game.actionbar.human"), humans)

        elapsedTimeTicks += timerTickDelay
        elapsedTimeSeconds = elapsedTimeTicks / 20
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
        player.inventory.setItem(EquipmentSlot.FEET, ItemStack(Material.LEATHER_BOOTS))
        player.inventory.setItem(EquipmentSlot.LEGS, ItemStack(Material.LEATHER_LEGGINGS))
        player.inventory.setItem(EquipmentSlot.CHEST, ItemStack(Material.LEATHER_CHESTPLATE))
        player.inventory.setItem(EquipmentSlot.HEAD, ItemStack(Material.ZOMBIE_HEAD))
        if (startInSpectator) { player.gameMode = GameMode.SPECTATOR }
    }
}