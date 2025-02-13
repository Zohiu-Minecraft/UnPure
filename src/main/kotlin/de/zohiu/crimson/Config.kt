package de.zohiu.crimson

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.IOException

class Config(crimson: Crimson, val name: String) {
    private val file: File = File("${crimson.configPath}${name}.yml")
    private val yamlConfig: FileConfiguration

    init {
        if (!file.exists()) {
            try {
                // TODO: Take default config from resources
                file.createNewFile();
            } catch (exception: IOException) {
                throw Exception("Config file cannot be created", exception)  // Make custom exception here in future
            }
        }

        yamlConfig = YamlConfiguration.loadConfiguration(file)
    }

    /**
     * Completely deletes this config
     */
    fun delete(): Boolean {
        if (!file.exists()) return false
        file.delete()
        return true
    }

    fun get(path: String): Any? {
        // TODO: Config caching
        return yamlConfig.get(path)
    }

    fun set(path: String, value: Any) {
        yamlConfig.set(path, value)
    }

    fun reload() {
        yamlConfig.load(file)
    }

    fun save() {
        yamlConfig.save(file)
    }

    /**
     * The paths array contains arrays with path and default.
     * e.g.: [["path", "default"], ["sword.damage", 10]]
     */
    fun init(paths: Array<Array<Any>>) {
        paths.forEach {
            val name = it[0] as String
            val default = it[1]

            if (!yamlConfig.contains(name)) {
                set(name, default)
            }
        }
        save()
    }
}