package de.zohiu.unpure.game

import de.zohiu.crimson.Effect
import de.zohiu.unpure.UnPure
import de.zohiu.unpure.chunkgenerator.VoidBiomeProvider
import de.zohiu.unpure.chunkgenerator.VoidChunkGenerator
import de.zohiu.unpure.config.Messages
import de.zohiu.unpure.events.GameEvents
import de.zohiu.unpure.data.CosmeticsData
import de.zohiu.unpure.data.DataOperations
import de.zohiu.unpure.data.StatisticsData
import me.neznamy.tab.api.TabAPI
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.*
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.io.File
import java.util.*


class Game (private val gameID: String, val host: Player) {
    companion object {
        @JvmStatic var openGames: MutableList<Game> = mutableListOf()

        fun createGame(host: Player): Game {
            return Game(UUID.randomUUID().toString(), host)
        }

        fun resetPlayer(player: Player) {
            player.gameMode = GameMode.ADVENTURE
            player.inventory.clear()
            player.health = 20.0
            player.saturation = 20.0f
            player.foodLevel = 20
            player.exp = 0.0f
            player.removePotionEffect(PotionEffectType.GLOWING)
            player.removePotionEffect(PotionEffectType.ABSORPTION)

            TabAPI.getInstance().getPlayer(player.uniqueId)?.let { TabAPI.getInstance().tabListFormatManager?.setPrefix(it, null) }
            TabAPI.getInstance().getPlayer(player.uniqueId)?.let { TabAPI.getInstance().nameTagManager?.setPrefix(it, null) }
            TabAPI.getInstance().getPlayer(player.uniqueId)?.setTemporaryGroup(null)
        }
    }

    val timerTickDelay = 5L
    val graceTimeSeconds = 60
    val gameTimeSeconds = 300
    val lastMinuteEffectsAtSecond = 30
    var elapsedTimeSeconds = 0
    var elapsedTimeTicks = 0L

    var starting = false
    var inProgress = false;
    var lastMinute = false;

    var bossbar: BossBar;
    var eventListener: GameEvents
    var world: World
    lateinit var gameTimer: Effect;

    var players: MutableList<Player> = mutableListOf()
    var spectators: MutableList<Player> = mutableListOf()
    var humans: MutableList<Player> = mutableListOf()
    var infected: MutableList<Player> = mutableListOf()
    var lastHuman: Player? = null

    var targetGamePath: String

    init {
        // Get random map
        val mapName: String = UnPure.maps.random()
        val mapPath = "${UnPure.mapsRoot}/${UnPure.templatesPath}/$mapName"
        targetGamePath = "${UnPure.gamesPath}/$gameID"

        // Clone the maps
        File(mapPath).copyRecursively(File("${UnPure.mapsRoot}/$targetGamePath"), overwrite = true)

        // Load the map
        val worldCreator = WorldCreator(targetGamePath);
        worldCreator.generator(VoidChunkGenerator())
        worldCreator.biomeProvider(VoidBiomeProvider())
        worldCreator.createWorld()
        world = Bukkit.getWorld(targetGamePath)!!

        // Initialize the world
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

        // Bossbar
        bossbar = Bukkit.createBossBar(this.gameID, BarColor.GREEN, BarStyle.SOLID)
        bossbar.setTitle(Messages.str("game.bossbar.default"))

        eventListener = GameEvents(this)
        openGames.add(this)
    }

    fun broadcast(message: String, targets: MutableList<Player>) {
        targets.forEach { it.sendMessage("${Messages.gameMessagePrefix}$message") }
    }

    fun rawBroadcast(message: String, targets: MutableList<Player>) {
        targets.forEach { it.sendMessage(message) }
    }

    fun title(title: String, subtitle: String, targets: MutableList<Player>) {
        targets.forEach { it.sendTitle(title, subtitle, 5, 100, 5) }
    }

    fun actionbar(message: String, targets: MutableList<Player>) {
        targets.forEach { it.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent(message)) }
    }

    fun start() {
        if (starting || inProgress) return
        starting = true

        // game countdown
        var i = 5
        UnPure.crimson.effectBuilder().repeat(5, 20) {
            rawBroadcast("${host.name}'s game is starting in ${i}s", players)
            i--
        }.run {
            starting = false
            if (players.size < 2) {
                rawBroadcast("Start aborted! There are not enough players online! (Min: 2)", players)
                return@run
            }
            inProgress = true

            // Hide game players for everyone else
            // See GameEvents for handling new player joins
            players.forEach { gamePlayer ->
                Bukkit.getOnlinePlayers().forEach { player ->
                    if (players.contains(player)) {
                        player.showPlayer(UnPure.instance, gamePlayer)
                    }
                    else {
                        player.hidePlayer(UnPure.instance, gamePlayer)
                    }
                }
            }

            // Copy players to humans and select first infected
            humans = players.toMutableList()
            humans.shuffle()
            infected.add(humans.removeLast())

            // init players
            players.forEach { bossbar.addPlayer(it) }
            humans.forEach {
                initHumanState(it);
                it.gameMode = GameMode.SPECTATOR
            }
            infected.forEach {
                initInfectedState(it);
                it.gameMode = GameMode.SPECTATOR
            }

            title(Messages.str("game.title.infected"), Messages.str("game.subtitle.infected"), infected)
            title(Messages.str("game.title.human"), Messages.str("game.subtitle.human"), humans)

            broadcast(Messages.str("game.status.starting-in", arrayOf("5")), players)

            // Wait a bit before starting the game
            UnPure.crimson.effectBuilder().wait(100).run {
                UnPure.instance.server.pluginManager.registerEvents(eventListener, UnPure.instance)

                broadcast(Messages.str("game.status.started"), players)

                bossbar.color = BarColor.GREEN
                humans.forEach {
                    it.teleport(world.spawnLocation.add(0.5, 0.0, 0.5))
                    it.gameMode = GameMode.SURVIVAL
                }
                infected.forEach {
                    it.teleport(UnPure.waitingArea.spawnLocation.add(0.5, 0.0, 0.5))
                    it.gameMode = GameMode.ADVENTURE
                }
                gracePeriod()
            }.start()
        }.start()
    }

    fun stop() {
        if (!inProgress) { return }
        inProgress = false
        heartbeatTask?.destroy()
        players.forEach {
            bossbar.removePlayer(it)
            if (it != lastHuman) resetPlayer(it)
        }
        bossbar.removeAll()
        gameTimer.destroy()
        StatisticsData.commitData()

        // Wait a bit before ending the game
        UnPure.crimson.effectBuilder().wait(200).run {
            val pClone = players.toList()
            players.clear()
            UnPure.waitingArea.players.forEach {
                if (!pClone.contains(it)) return@forEach
                resetPlayer(it)
                UnPure.teleportToLobby(it)
            }
            world.players.forEach {
                resetPlayer(it)
                UnPure.teleportToLobby(it)
            }
            players.clear()
            openGames.remove(this)
            HandlerList.unregisterAll(eventListener)
            Bukkit.unloadWorld(world, false)
            File("${UnPure.mapsRoot}/$targetGamePath").deleteRecursively()
        }.start()
    }

    fun humanWin() {
        broadcast(Messages.str("game.coin"), humans)
        humans.forEach {
            Cosmetics.winEffect(it)
            DataOperations.addOneToTable(it, StatisticsData.tableHumanWins)
            DataOperations.addOneToTable(it, CosmeticsData.tableCoins)
        }
        infected.forEach { DataOperations.addOneToTable(it, StatisticsData.tableInfectedLosses) }
        stop()
    }

    fun infectedWin() {
        infected.forEach { DataOperations.addOneToTable(it, StatisticsData.tableInfectedWins) }
        stop()
    }

    fun infectPlayer(player: Player, killer: Player? = null) {
        if (infected.contains(player)) {  // Already infected
            DataOperations.addOneToTable(player, StatisticsData.tableDeaths)
            initInfectedState(player)
            player.gameMode = GameMode.SURVIVAL
            return
        }
        if (!humans.contains(player)) { return }  // Just to be safe.

        // Human got killed here
        DataOperations.addOneToTable(player, StatisticsData.tableDeaths)

        if (killer != null) {
            broadcast(Cosmetics.killMessage(killer, player), players)
            DataOperations.addOneToTable(killer, StatisticsData.tableKills)
        } else {
            broadcast(Messages.str("game.player.self-infected", arrayOf(player.name)), players)
        }

        Cosmetics.deathEffect(player, player.location)
        humans.remove(player)
        infected.add(player)
        title(Messages.str("game.title.infection"), Messages.str("game.subtitle.infected"), mutableListOf(player))

        // Game ends when no humans left
        if (humans.isEmpty()) {
            if (killer is Player) Cosmetics.winEffect(killer)
            broadcast(Messages.str("game.status.end.humans-dead"), players)
            lastHuman = player
            player.gameMode = GameMode.SPECTATOR
            infectedWin()
        } else {
            initInfectedState(player)
            player.gameMode = GameMode.SURVIVAL
        }
    }

    fun removePlayer(player: Player) {
        if (players.contains(player)) { players.remove(player) }
        resetPlayer(player)
        UnPure.teleportToLobby(player)
        broadcast(Messages.str("game.status.player-left", arrayOf(player.name)), players)

        if (humans.contains(player)) {
            humans.remove(player)
            if (humans.isEmpty()) {
                broadcast(Messages.str("game.status.end.last-human-left"), players)
                infectedWin()
            }
        }
        if (infected.contains(player)) {
            infected.remove(player)
            if (infected.isEmpty()) {
                broadcast(Messages.str("game.status.end.last-infected-left"), players)
                humanWin()
            }
        }
    }

    fun enableSpectate(player: Player, target: Player) {
        spectators.add(player)
        player.gameMode = GameMode.SPECTATOR
        player.teleport(target.location)
        TabAPI.getInstance().getPlayer(player.uniqueId)?.let { TabAPI.getInstance().tabListFormatManager?.setPrefix(it, "&7&l&oSpectator &r&7&o") }
        TabAPI.getInstance().getPlayer(player.uniqueId)?.let { TabAPI.getInstance().nameTagManager?.setPrefix(it, "&7&l&oSpectator &r&7&o") }
        TabAPI.getInstance().getPlayer(player.uniqueId)?.setTemporaryGroup("afk")  // Spectators get sorted at the bottom
        // Show game players and hide outside players
        Bukkit.getOnlinePlayers().forEach {
            if (players.contains(it)) player.showPlayer(UnPure.instance, it)
            else player.hidePlayer(UnPure.instance, it)
        }
        players.forEach { it.showPlayer(UnPure.instance, player) }
    }

    fun disableSpectate(player: Player) {
        spectators.remove(player)
        UnPure.teleportToLobby(player)
        TabAPI.getInstance().getPlayer(player.uniqueId)?.let { TabAPI.getInstance().tabListFormatManager?.setPrefix(it, null) }
        TabAPI.getInstance().getPlayer(player.uniqueId)?.let { TabAPI.getInstance().nameTagManager?.setPrefix(it, null) }
        TabAPI.getInstance().getPlayer(player.uniqueId)?.setTemporaryGroup(null)
        // Hide game players (showing outside players happens in GlobalEvents when changing worlds)
        players.forEach { it.hidePlayer(UnPure.instance, player) }
    }

    private fun gracePeriod() {
        gameTimer = UnPure.crimson.effectBuilder().repeatUntil({ elapsedTimeTicks > graceTimeSeconds * 20 }, timerTickDelay) {
            val remaining = graceTimeSeconds - elapsedTimeSeconds
            bossbar.setTitle(Messages.str("game.bossbar.grace", arrayOf(remaining.toString())))
            bossbar.progress = Math.max(0.0, 1 - elapsedTimeTicks / (graceTimeSeconds * 20.0))
            gameTick()
        }.run {
            gamePeriod()
        }.start()
    }

    private fun gamePeriod() {
        gameTimer.destroy()  // Make sure that the grace period is actually stopped for sure

        infected.forEach {
            initInfectedState(it)
            it.gameMode = GameMode.SURVIVAL
        }
        spectators.forEach {
            if (it.world == UnPure.waitingArea) it.teleport(world.spawnLocation.add(0.5, 0.0, 0.5))
        }

        broadcast(Messages.str("game.status.grace-over"), players)
        bossbar.color = BarColor.YELLOW

        gameTimer = UnPure.crimson.effectBuilder().repeatUntil({ elapsedTimeTicks > (gameTimeSeconds * 20) + (graceTimeSeconds * 20) }, timerTickDelay) {
            val remaining = gameTimeSeconds - elapsedTimeSeconds + graceTimeSeconds
            if (remaining == lastMinuteEffectsAtSecond && !lastMinute) {
                lastMinute = true
                broadcast(Messages.str("game.status.last-minute"), players)
                bossbar.color = BarColor.RED
                players.forEach {
                    it.addPotionEffect(PotionEffect(PotionEffectType.GLOWING, PotionEffect.INFINITE_DURATION, 0))
                }
            }
            bossbar.setTitle(Messages.str("game.bossbar.game-progress", arrayOf(remaining.toString())))
            bossbar.progress = Math.max(0.0, 1 - (elapsedTimeTicks - (graceTimeSeconds * 20.0)) / (gameTimeSeconds * 20.0))
            gameTick()
        }.run {
            broadcast(Messages.str("game.status.end.timeout"), players)
            humanWin()
        }.start()

        heartbeatTask = startHeartbeatTask()
    }

    // Every timer tick
    private fun gameTick() {
        actionbar(Messages.str("game.actionbar.infected"), infected)
        actionbar(Messages.str("game.actionbar.human"), humans)

        elapsedTimeTicks += timerTickDelay
        elapsedTimeSeconds = (elapsedTimeTicks / 20L).toInt()
        infected.forEach { Cosmetics.trail(it) }
    }

    private fun initHumanState(player: Player) {
        resetPlayer(player)
        player.teleport(world.spawnLocation.add(0.5, 0.0, 0.5))
        player.inventory.addItem(ItemStack(Material.WOODEN_SWORD))
        player.inventory.addItem(ItemStack(Material.WOODEN_PICKAXE))
        player.inventory.addItem(ItemStack(Material.WOODEN_AXE))
        player.inventory.addItem(ItemStack(Material.WOODEN_SHOVEL))
        player.inventory.addItem(ItemStack(Material.GOLDEN_APPLE))
        TabAPI.getInstance().getPlayer(player.uniqueId)?.let { TabAPI.getInstance().tabListFormatManager?.setPrefix(it, "&e&lHuman &r&e") }
        TabAPI.getInstance().getPlayer(player.uniqueId)?.let { TabAPI.getInstance().nameTagManager?.setPrefix(it, "&e&lHuman &r&e") }
        TabAPI.getInstance().getPlayer(player.uniqueId)?.setTemporaryGroup("human")
    }

    private fun initInfectedState(player: Player) {
        resetPlayer(player)
        player.teleport(world.spawnLocation.add(0.5, 0.0, 0.5))
        player.addPotionEffect(PotionEffect(PotionEffectType.GLOWING, PotionEffect.INFINITE_DURATION, 0))
        player.inventory.addItem(ItemStack(Material.WOODEN_AXE))
        player.inventory.addItem(ItemStack(Material.STONE_PICKAXE))
        Cosmetics.infectedOutfit(player)
        TabAPI.getInstance().getPlayer(player.uniqueId)?.let { TabAPI.getInstance().tabListFormatManager?.setPrefix(it, "&2&lInfected &r&2") }
        TabAPI.getInstance().getPlayer(player.uniqueId)?.let { TabAPI.getInstance().nameTagManager?.setPrefix(it, "&2&lInfected &r&2") }
        TabAPI.getInstance().getPlayer(player.uniqueId)?.setTemporaryGroup("infected")
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
                    notint.center = world.spawnLocation.add(0.5, 0.0, 0.5)
                    notint.size = 1024.0
                    notint.warningDistance = 0

                    human.worldBorder = notint
                }
            }
        }
    }
}