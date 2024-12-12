package de.zohiu.unpure

import de.zohiu.unpure.commands.GoToMap
import de.zohiu.unpure.commands.LoadMap
import de.zohiu.unpure.commands.Start
import de.zohiu.unpure.commands.UnloadMap
import de.zohiu.unpure.game.Game
import de.zohiu.unpure.events.GlobalEvents
import de.zohiu.unpure.events.LobbyEvents
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class UnPure : JavaPlugin() {
    companion object {
        @JvmStatic lateinit var maps: Array<String>;
        @JvmStatic lateinit var instance: UnPure;

        @JvmStatic lateinit var lobby: World;

        // All maps are in the "maps" directory.
        // Spigot opens new maps relative to the main world.
        // Since the main world is in maps/lobby, "maps" turns into the new root when creating worlds.
        // That's why the other paths do not have the mapsRoot prefix.
        @JvmStatic var mapsRoot = "maps";
        @JvmStatic var lobbyMapPath = "maps/lobby";
        @JvmStatic var templatesPath = "templates";
        @JvmStatic var gamesPath = "games";
    }

    override fun onEnable() {
        instance = this;
        maps = File("maps/$templatesPath").listFiles()?.map { it.name }?.toTypedArray() ?: arrayOf();
        lobby = Bukkit.getWorld(lobbyMapPath)!!;

        reset()

        this.getCommand("loadmap")?.setExecutor(LoadMap());
        this.getCommand("unloadmap")?.setExecutor(UnloadMap());
        this.getCommand("gotomap")?.setExecutor(GoToMap());
        this.getCommand("start")?.setExecutor(Start());

        this.server.pluginManager.registerEvents(GlobalEvents(), this)
        this.server.pluginManager.registerEvents(LobbyEvents(), this)
    }

    override fun onDisable() {
        reset()
    }

    private fun reset() {
        // Stop all games
        Game.openGames.forEach { it.stop() }

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
