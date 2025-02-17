package de.zohiu.crimson

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.*
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLException


enum class CacheLevel {
    GET, PERIODIC, FULL
}

class CrimsonDatabaseException(message: String) : RuntimeException(message)

class Database(internal val crimson: Crimson, val name: String, cacheLevel: CacheLevel, private val maxCacheSize: Int,
               period: Long?, private val periodCondition: () -> Boolean) : AutoCloseable {
    internal var connection: Connection
    internal val getCache: HashMap<String, LinkedHashMap<String, Any>> = HashMap()
    internal val writeCache: HashMap<String, MutableList<PreparedStatement>> = HashMap()

    internal var getCached = false
    internal var writeCached = false

    private var periodicEffect: Effect? = null
    internal val coroutineScope: CoroutineScope = MainScope()

    internal val mapper = jacksonObjectMapper()

    init {
        val url = "jdbc:sqlite:${crimson.dataPath}${name}.db"
        try {
            connection = DriverManager.getConnection(url)
            connection.autoCommit = false
            crimson.databaseConnections.add(this)
        } catch (e: SQLException) {
            if (e.message != null) throw CrimsonDatabaseException(e.message!!)
            else throw e
        }

        when(cacheLevel) {
            CacheLevel.GET -> {
                getCached = true
            }
            CacheLevel.PERIODIC -> {
                writeCached = true
                getCached = true
                if (period == null) throw CrimsonDatabaseException("No period specified.")
                periodicEffect = crimson.effectBuilder().repeatForever(period) {
                    if (periodCondition.invoke()) asyncCommitCache()
                }.start()
            }
            CacheLevel.FULL -> {
                writeCached = true
                getCached = true
            }
        }
    }

    override fun close() {
        periodicEffect?.destroy()
        commitCache()
        connection.close()
        getCache.clear()
        coroutineScope.cancel()
        crimson.databaseConnections.remove(this)
    }

    fun asyncCommitCache() {
        coroutineScope.launch(Dispatchers.IO) {
            commitCache()
        }
    }

    fun commitCache() {
        if (!writeCached || writeCache.size == 0) return
        writeCache.keys.forEach { key ->
            writeCache[key]!!.forEach { statement ->
                try {
                    statement.execute()
                    statement.close()
                } catch (e: Exception) {
                    if (e.message != null) throw CrimsonDatabaseException(e.message!!)
                    else throw e
                }
            }
            writeCache[key] = mutableListOf()
        }
        connection.commit()
    }

    internal fun commit(statement: PreparedStatement) {
        try {
            statement.executeUpdate()
            statement.close()
            connection.commit()
        } catch (e: Exception) {
            if (e.message != null) throw CrimsonDatabaseException(e.message!!)
            else throw e
        }
    }

    fun getTable(name: String) : Table {
        // Underscore before name to allow table names to start with numbers
        val internalName = "_$name"
        val statement: PreparedStatement = connection.prepareStatement(
            "CREATE TABLE IF NOT EXISTS $internalName (key STRING NOT NULL PRIMARY KEY, type STRING, value STRING)"
        )
        commit(statement)

        // Create linked hash map for table get cache
        if (getCached && !getCache.contains(internalName)) {
            getCache[internalName] = object : LinkedHashMap<String, Any>() {
                // Set correct max cache size
                override fun removeEldestEntry(eldest: Map.Entry<String, Any>?): Boolean {
                    return size > maxCacheSize
                }
            }
        }

        // Create mutable list for table write cache
        if (writeCached && !writeCache.contains(internalName)) {
            writeCache[internalName] = mutableListOf()
        }

        return Table(this, internalName)
    }
}


class Table(val database: Database, private val internalName: String) {
    fun set (key: String, value: Any) {
        if (database.getCached) {
            database.getCache[internalName]!![key] = value
        }

        database.coroutineScope.launch(Dispatchers.IO) {
            try {
                val statement: PreparedStatement = database.connection.prepareStatement(
                    "INSERT OR REPLACE INTO $internalName (key, type, value) VALUES (?,?,?)"
                )

                statement.setString(1, key)
                statement.setString(2, value.javaClass.name)
                statement.setString(3, database.mapper.writeValueAsString(value))

                if (database.writeCached) database.writeCache[internalName]!!.add(statement)
                else database.commit(statement)
            } catch (e: Exception) {
                if (e.message != null) throw CrimsonDatabaseException(e.message!!)
                else throw e
            }
        }
    }

    fun get(key: String) : Any? {
        if (database.getCached && database.getCache[internalName]!!.contains(key)) {
            return database.getCache[internalName]!![key]
        }

        val statement: PreparedStatement = database.connection.prepareStatement(
            "SELECT type, value FROM $internalName WHERE key = ?"
        )
        statement.setString(1, key)

        var type: String? = null
        var value: String? = null
        statement.executeQuery().use { rs ->
            if (rs.next()) {
                type = rs.getString("type") // Get the BLOB data
                value = rs.getString("value") // Get the BLOB data
            }
        }

        if (type === null || value === null) {
            return null
        }

        val result = database.mapper.readValue(value, Class.forName(type))
        if (database.getCached) database.getCache[internalName]!![key] = result
        return result
    }
}