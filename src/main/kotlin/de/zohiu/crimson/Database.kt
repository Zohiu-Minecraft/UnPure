package de.zohiu.crimson

import org.bukkit.Bukkit
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLException

// PERIODIC does not work yet!
enum class CacheLevel() {
    NONE, GET, PERIODIC, FULL
}

class Database(val crimson: Crimson, val name: String, cacheLevel: CacheLevel, val maxCacheSize: Int,
               period: Int?, private val periodCondition: () -> Boolean) : AutoCloseable {
    lateinit var connection: Connection
    val getCache: HashMap<String, LinkedHashMap<String, Any>> = HashMap()
    val writeCache: HashMap<String, MutableList<PreparedStatement>> = HashMap()

    var getCached = false
    var writeCached = false
    private var periodicEffect: Effect? = null

    init {
        val url = "jdbc:sqlite:${crimson.dataPath}${name}.db"
        try {
            connection = DriverManager.getConnection(url)
            connection.autoCommit = false
            val statement: PreparedStatement = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS crimson_meta (key STRING NOT NULL PRIMARY KEY, value STRING)"
            )
            commit(statement)
            crimson.databaseConnections.add(this)
        } catch (e: SQLException) {
            println(e.message)
        }

        when(cacheLevel) {
            CacheLevel.NONE -> {}
            CacheLevel.GET -> {
                getCached = true
            }
            CacheLevel.PERIODIC -> {
                writeCached = true
                getCached = true
                if (period == null) throw RuntimeException("No period specified.")
                periodicEffect = crimson.effectBuilder().repeatForever(period) {
                    if (periodCondition.invoke()) commitCache()
                }.start()
            }
            CacheLevel.FULL -> {
                writeCached = true
                getCached = true
            }
        }
    }

    override fun close() {
        periodicEffect?.abort()
        commitCache()
        connection.close()
        getCache.clear()
        crimson.databaseConnections.remove(this)
    }

    fun commitCache() {
        if (writeCached) {
            writeCache.keys.forEach { key ->
                writeCache[key]!!.forEach { statement ->
                    statement.execute()
                    statement.close()
                }
                writeCache[key] = mutableListOf()
            }
            connection.commit()
        }
    }

    fun commit(statement: PreparedStatement) {
        statement.executeUpdate()
        statement.close()
        connection.commit()
    }

    fun getTable(name: String) : Table {
        // Underscore before name to allow table names to start with numbers
        val statement: PreparedStatement = connection.prepareStatement(
            "CREATE TABLE IF NOT EXISTS _$name (key STRING NOT NULL PRIMARY KEY, value BLOB)"
        )
        commit(statement)
        val internalName = "_$name"
        // Create linked hash map for table if not exists
        if (getCached && !getCache.contains(internalName)) getCache[internalName] = object : LinkedHashMap<String, Any>() {
            override fun removeEldestEntry(eldest: Map.Entry<String, Any>?): Boolean {
                return size > maxCacheSize
            }
        };
        if (writeCached && !writeCache.contains(internalName)) writeCache[internalName] = mutableListOf()
        return Table(this, internalName)
    }
}


class Table(val database: Database, private val internalName: String) {
    fun set(key: String, value: Any) {
        if (database.getCached) database.getCache[internalName]!![key] = value

        ByteArrayOutputStream().use { byteStream ->
            ObjectOutputStream(byteStream).use { objectStream ->
                objectStream.writeObject(value)

                val statement: PreparedStatement = database.connection.prepareStatement(
                    "INSERT OR REPLACE INTO $internalName (key, value) VALUES (?,?)"
                )
                statement.setString(1, key)
                statement.setBytes(2, byteStream.toByteArray())
                if (database.writeCached) database.writeCache[internalName]!!.add(statement)
                else database.commit(statement)
            }
        }
    }

    fun get(key: String) : Any? {
        if (database.getCached && database.getCache[internalName]!!.contains(key)) {
            return database.getCache[internalName]!![key]
        }

        val statement: PreparedStatement = database.connection.prepareStatement(
            "SELECT value FROM $internalName WHERE key = ?"
        )
        statement.setString(1, key)

        var data: ByteArray? = null
        statement.executeQuery().use { rs ->
            if (rs.next()) {
                data = rs.getBytes("value") // Get the BLOB data
            }
        }

        if (data === null) {
            return null
        }

        ByteArrayInputStream(data).use { byteStream ->
            ObjectInputStream(byteStream).use { objectStream ->
                val result = objectStream.readObject()
                if (database.getCached) database.getCache[internalName]!![key] = result
                return result
            }
        }
    }
}