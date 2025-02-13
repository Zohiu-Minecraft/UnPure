package de.zohiu.crimson

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLException
import java.time.Instant

enum class CacheLevel {
    NONE, GET, PERIODIC, FULL
}

class Database(val crimson: Crimson, val name: String) : AutoCloseable {
    lateinit var connection: Connection
    init {
        val url = "jdbc:sqlite:${crimson.dataPath}${name}.db"
        try {
            connection = DriverManager.getConnection(url)
            val statement: PreparedStatement = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS crimson_meta (key STRING NOT NULL PRIMARY KEY, value STRING)"
            )
            commit(statement)
            crimson.databaseConnections.add(this)
        } catch (e: SQLException) {
            println(e.message)
        }
    }

    override fun close() {
        connection.close()
        crimson.databaseConnections.remove(this)
    }

    fun commit(statement: PreparedStatement) {
        statement.executeUpdate()
        statement.close()

        val updateStatement: PreparedStatement = connection.prepareStatement(
            "INSERT OR REPLACE INTO crimson_meta (key, value) VALUES (?,?)"
        )
        updateStatement.setString(1, "updated")
        updateStatement.setString(2, Instant.now().toString())
        updateStatement.executeUpdate()
        updateStatement.close()
    }

    fun getTable(name: String) : Table {
        // Underscore before name to allow table names to start with numbers
        val statement: PreparedStatement = connection.prepareStatement(
            "CREATE TABLE IF NOT EXISTS _$name (key STRING NOT NULL PRIMARY KEY, value BLOB)"
        )
        commit(statement)
        return Table(this, "_$name")
    }
}


class Table(val database: Database, private val internalName: String) {
    fun set(key: String, value: Any?) {
        ByteArrayOutputStream().use { byteStream ->
            ObjectOutputStream(byteStream).use { objectStream ->
                objectStream.writeObject(value)

                val statement: PreparedStatement = database.connection.prepareStatement(
                    "INSERT OR REPLACE INTO $internalName (key, value) VALUES (?,?)"
                )
                statement.setString(1, key)
                statement.setBytes(2, byteStream.toByteArray())
                database.commit(statement)
            }
        }
    }

    fun get(key: String) : Any? {
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
                return objectStream.readObject()
            }
        }
    }
}