package de.zohiu.unpure.game;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.File;

import static de.zohiu.unpure.UnPure.lobby;

public class test {
    private void reset() {
        // Stop all games
        for (Game game : Game.getOpenGames()) {
            game.stop();
        }

        // Unload all remaining worlds
        for (World world : Bukkit.getWorlds()) {
            if (!world.equals(lobby)) {
                for (Player player : world.getPlayers()) {
                    player.teleport(lobby.getSpawnLocation());
                }
                Bukkit.unloadWorld(world, true);
            }
        }

        // Delete all existing games
        File gamesDirectory = new File(mapsRoot + "/" + gamesPath);
        deleteRecursively(gamesDirectory);
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            for (File subFile : file.listFiles()) {
                deleteRecursively(subFile);
            }
        }
}
