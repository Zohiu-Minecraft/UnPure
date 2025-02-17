package de.zohiu.unpure

import de.zohiu.crimson.Crimson
import de.zohiu.unpure.chunkgenerator.VoidBiomeProvider
import de.zohiu.unpure.chunkgenerator.VoidChunkGenerator
import de.zohiu.unpure.commands.game.Leave
import de.zohiu.unpure.commands.game.Spectate
import de.zohiu.unpure.commands.game.SpectateTabComplete
import de.zohiu.unpure.commands.game.Start
import de.zohiu.unpure.commands.general.AFK
import de.zohiu.unpure.commands.worlds.*
import de.zohiu.unpure.data.DataOperations
import de.zohiu.unpure.data.Placeholders
import de.zohiu.unpure.data.StatisticsData
import de.zohiu.unpure.events.GlobalEvents
import de.zohiu.unpure.events.LobbyEvents
import de.zohiu.unpure.events.WaitingAreaEvents
import de.zohiu.unpure.game.Game
import de.zohiu.unpure.lobby.Crate
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File


class UnPure : JavaPlugin() {
    companion object {
        val playtimeCounter: HashMap<Player, Long> = HashMap()
        fun updatePlaytime(player: Player) {
            val playtime = System.currentTimeMillis() - playtimeCounter[player]!!
            playtimeCounter[player] = System.currentTimeMillis()
            DataOperations.addNToTable(player, StatisticsData.tablePlaytime, playtime)
        }

        @JvmStatic lateinit var maps: Array<String>;
        @JvmStatic lateinit var instance: UnPure;

        @JvmStatic lateinit var waitingArea: World;
        @JvmStatic lateinit var lobby: World;
        fun teleportToLobby(player: Player) {
            val loc = lobby.spawnLocation.add(0.5, 0.0, 0.5)
            player.teleport(loc)
            player.inventory.clear()
            player.gameMode = GameMode.ADVENTURE
        }

        // All maps are in the "maps" directory.
        // Spigot opens new maps relative to the main world.
        // Since the main world is in maps/lobby, "maps" turns into the new root when creating worlds.
        // That's why the other paths do not have the mapsRoot prefix.
        @JvmStatic var mapsRoot = "maps";
        @JvmStatic var lobbyMapPath = "maps/lobby";
        @JvmStatic var waitingAreaPath = "maps/waitingarea";
        @JvmStatic var templatesPath = "templates";
        @JvmStatic var gamesPath = "games";
        lateinit var crimson: Crimson
        lateinit var placeholders: Placeholders
    }

    override fun onEnable() {
        instance = this;
        crimson = Crimson(this)
        maps = File("maps/$templatesPath").listFiles()?.map { it.name }?.toTypedArray() ?: arrayOf();
        lobby = Bukkit.getWorld(lobbyMapPath)!!;

        reset()

        // Load waiting area
        val worldCreator = WorldCreator(waitingAreaPath);
        worldCreator.generator(VoidChunkGenerator())
        worldCreator.biomeProvider(VoidBiomeProvider())
        worldCreator.createWorld()
        waitingArea = Bukkit.getWorld(waitingAreaPath)!!;

        this.getCommand("createtemplate")?.setExecutor(CreateTemplate())
        this.getCommand("loadmap")?.setExecutor(LoadMap())
        this.getCommand("loadmap")?.tabCompleter = LoadMapTabComplete()
        this.getCommand("unloadmap")?.setExecutor(UnloadMap())
        this.getCommand("unloadmap")?.tabCompleter = UnloadMapTabComplete()
        this.getCommand("gotomap")?.setExecutor(GoToMap())
        this.getCommand("gotomap")?.tabCompleter = GoToMapTabComplete()
        this.getCommand("spectate")?.setExecutor(Spectate())
        this.getCommand("spectate")?.tabCompleter = SpectateTabComplete()
        this.getCommand("start")?.setExecutor(Start())
        this.getCommand("leave")?.setExecutor(Leave())
        this.getCommand("afk")?.setExecutor(AFK())


        // Start playtime new in case this is a reload
        Bukkit.getOnlinePlayers().forEach {
            playtimeCounter[it] = System.currentTimeMillis()
        }

        // Save playtime to DB every 5 minutes
        crimson.effectBuilder().repeatForever(20 * 60 * 5) {
            Bukkit.getOnlinePlayers().forEach {
                updatePlaytime(it)
            }
        }.start()

        this.server.pluginManager.registerEvents(GlobalEvents(), this)
        this.server.pluginManager.registerEvents(LobbyEvents(), this)
        this.server.pluginManager.registerEvents(WaitingAreaEvents(), this)
        this.server.pluginManager.registerEvents(Crate(), this)

        placeholders = Placeholders()
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            placeholders.register()
        }

        // Music player reload world radio
        Bukkit.getServer().dispatchCommand(Bukkit.getServer().consoleSender, "wr reload")
    }

    override fun onDisable() {
        placeholders.unregister()
        reset()
        crimson.cleanup()
    }

    private fun reset() {
        // Stop all games
        Game.openGames.forEach { it.stop() }

        // Make sure broken games are ended too
        Bukkit.getOnlinePlayers().forEach { player ->
            Game.resetPlayer(player)
            if (player.hasPermission("unpure.admin")) player.gameMode = GameMode.CREATIVE
            Bukkit.getBossBars().forEach { bossBar ->
                bossBar.removeAll()
            }
        }

        // Unload all remaining worlds
        Bukkit.getWorlds().forEach { world ->
            if (world != lobby) {
                world.players.forEach { it.teleport(lobby.spawnLocation) }
                Bukkit.unloadWorld(world, true)
            }
        }

        // Delete all existing games
        File("$mapsRoot/$gamesPath").deleteRecursively()
    }
}
