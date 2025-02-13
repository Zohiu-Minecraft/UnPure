package de.zohiu.crimson

import org.bukkit.Bukkit
import org.bukkit.event.HandlerList
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class Crimson(val plugin: JavaPlugin) {
    private val pluginFolder = plugin.dataFolder
    val configPath = "${pluginFolder}${File.separator}config${File.separator}"
    val dataPath = "${pluginFolder}${File.separator}data${File.separator}"

    val runningEffects: MutableList<Effect> = mutableListOf()
    val databaseConnections: MutableList<Database> = mutableListOf()

    init {
        File(configPath).mkdirs()
        File(dataPath).mkdirs()
    }

    fun getConfig(name: String): Config {
        return Config(this, name)
    }

    fun getDatabase(name: String): Database {
        return Database(this, name)
    }

    fun effectBuilder() : Effect {
        return Effect(this, plugin)
    }


    fun cleanup() {
        // Unregister all events to make /reload work
        HandlerList.unregisterAll(plugin)
        runningEffects.toMutableList().forEach { it.abort() }
        databaseConnections.toMutableList().forEach { it.close() }
    }
}