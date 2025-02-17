package de.zohiu.crimson

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

    fun getDatabase(name: String, cacheLevel: CacheLevel, maxCacheSize: Int = 500, period: Long? = null,
                    periodCondition: () -> Boolean = { true }): Database {
        return Database(this, name, cacheLevel, maxCacheSize, period, periodCondition)
    }

    fun effectBuilder() : Effect {
        return Effect(this, plugin)
    }


    fun cleanup() {
        // Unregister all events to make /reload work
        HandlerList.unregisterAll(plugin)
        runningEffects.toMutableList().forEach { it.destroy() }
        databaseConnections.toMutableList().forEach { it.close() }
    }
}