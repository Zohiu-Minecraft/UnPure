package de.zohiu.unpure.data

import de.zohiu.crimson.Table
import org.bukkit.entity.Player

class DataOperations {
    companion object {
        fun addOneToTable(player: Player, table: Table) {
            var current = table.get(player.uniqueId.toString())
            if (current == null) current = 0L
            table.set(player.uniqueId.toString(), current as Long + 1L)
        }

        fun addNToTable(player: Player, table: Table, num: Long) {
            var current = table.get(player.uniqueId.toString())
            if (current == null) current = num
            table.set(player.uniqueId.toString(), current as Long + num)
        }

        fun addNToTable(player: Player, table: Table, num: Int) {
            var current = table.get(player.uniqueId.toString())
            if (current == null) current = num.toLong()
            table.set(player.uniqueId.toString(), current as Long + num.toLong())
        }

        fun getTableInt(player: Player, table: Table): Int {
            var current = table.get(player.uniqueId.toString())
            if (current == null) {
                current = 0L
                table.set(player.uniqueId.toString(), current)
            }
            return (current as Long).toInt()
        }

        fun getTableLong(player: Player, table: Table): Long {
            var current = table.get(player.uniqueId.toString())
            if (current == null) {
                current = 0L
                table.set(player.uniqueId.toString(), current)
            }
            return current as Long
        }
    }
}