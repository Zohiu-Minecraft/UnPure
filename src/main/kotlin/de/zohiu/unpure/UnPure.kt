package de.zohiu.unpure

import de.zohiu.crimson.Crimson
import de.zohiu.unpure.chunkgenerator.VoidBiomeProvider
import de.zohiu.unpure.chunkgenerator.VoidChunkGenerator
import de.zohiu.unpure.commands.*
import de.zohiu.unpure.game.Game
import de.zohiu.unpure.events.GlobalEvents
import de.zohiu.unpure.events.LobbyEvents
import de.zohiu.unpure.lobby.Crate
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class UnPure : JavaPlugin() {
    companion object {
        @JvmStatic lateinit var maps: Array<String>;
        @JvmStatic lateinit var instance: UnPure;

        @JvmStatic lateinit var lobby: World;
        @JvmStatic lateinit var waitingarea: World;

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
    }

    override fun onEnable() {
        instance = this;
        crimson = Crimson(this)
        maps = File("maps/$templatesPath").listFiles()?.map { it.name }?.toTypedArray() ?: arrayOf();
        lobby = Bukkit.getWorld(lobbyMapPath)!!;

        // Load waiting area
        val worldCreator = WorldCreator(waitingAreaPath);
        worldCreator.generator(VoidChunkGenerator())
        worldCreator.biomeProvider(VoidBiomeProvider())
        worldCreator.createWorld()
        waitingarea = Bukkit.getWorld(waitingAreaPath)!!;

        reset()

        this.getCommand("createtemplate")?.setExecutor(CreateTemplate())
        this.getCommand("loadmap")?.setExecutor(LoadMap())
        this.getCommand("loadmap")?.tabCompleter = LoadMapTabComplete()
        this.getCommand("unloadmap")?.setExecutor(UnloadMap())
        this.getCommand("unloadmap")?.tabCompleter = UnloadMapTabComplete()
        this.getCommand("gotomap")?.setExecutor(GoToMap())
        this.getCommand("gotomap")?.tabCompleter = GoToMapTabComplete()
        this.getCommand("start")?.setExecutor(Start())

        this.server.pluginManager.registerEvents(GlobalEvents(), this)
        this.server.pluginManager.registerEvents(LobbyEvents(), this)
        this.server.pluginManager.registerEvents(Crate(), this)
    }

    override fun onDisable() {
        reset()
        crimson.cleanup()
    }

    private fun reset() {
        // Stop all games
        Game.openGames.forEach { it.stop() }

        // Unload all remaining worlds
        Bukkit.getWorlds().forEach { world ->
            if (world != lobby && world != waitingarea) {
                world.players.forEach { it.teleport(lobby.spawnLocation) }
                Bukkit.unloadWorld(world, true)
            }
        }

        // Delete all existing games
        File("$mapsRoot/$gamesPath").deleteRecursively()
    }
}
