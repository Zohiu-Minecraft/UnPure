package de.zohiu.crimson

import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.util.ArrayDeque


sealed class Action {
    data class Task(val task: () -> Unit) : Action()
    data class Delay(val delay: Long) : Action()
    data class Repeat(val task: () -> Unit, val expression: () -> Boolean, val amount: Int, val delay: Long) : Action()
    data class RepeatAmbiguous(val task: () -> Unit, val expression: () -> Boolean, val delay: Long) : Action()
    data class RepeatForever(val task: () -> Unit, val expression: () -> Boolean, val delay: Long) : Action()
}

class Effect(val crimson: Crimson, val plugin: JavaPlugin) {
    private val actions: MutableList<Action> = mutableListOf()
    private val actionQueue = ArrayDeque<Action>()
    private val tasks: MutableList<BukkitTask> = mutableListOf()

    private var abort = false
    private var offset: Long = 0

    private fun bakeTask(task: () -> Unit, timeOffsetTicks: Long): BukkitTask {
        return object : BukkitRunnable() { override fun run() {
            if (abort) return
            task()
        }}.runTaskLater(plugin, timeOffsetTicks)
    }

    fun run(task: () -> Unit): Effect {
        actions.add(Action.Task(task = task))
        return this
    }

    fun repeat(amount: Int, delay: Long, task: () -> Unit): Effect {
        actions.add(Action.Repeat(task = task, expression = { true }, amount = amount, delay = delay))
        return this
    }

    fun repeatWhile(expression: () -> Boolean, delay: Long, task: () -> Unit): Effect {
        actions.add(Action.RepeatAmbiguous(task = task, expression = expression, delay = delay))
        return this
    }

    fun repeatUntil(expression: () -> Boolean, delay: Long, task: () -> Unit): Effect {
        return repeatWhile({ !expression() }, delay, task)
    }

    fun repeatForever(delay: Long, task: () -> Unit): Effect {
        actions.add(Action.RepeatForever(task = task, expression = { true }, delay = delay))
        return this
    }

    fun wait(delay: Long): Effect {
        actions.add(Action.Delay(delay = delay))
        return this
    }


    fun start(restart: Boolean = true): Effect {
        if (!plugin.isEnabled) return this
        if (!crimson.runningEffects.contains(this)) crimson.runningEffects.add(this)
        if (restart) stop()

        while (actionQueue.size > 0) {
            if (abort) return this
            val action = actionQueue.pollFirst() // ?: break

            when(action) {
                is Action.Task -> tasks.add(bakeTask(action.task, offset))
                is Action.Delay -> offset += action.delay
                is Action.Repeat -> repeat(action.amount) {
                    tasks.add(bakeTask(action.task, offset))
                    offset += action.delay
                }

                // This one can't be baked because it takes an undefined amount of time
                // That's why it breaks the loop and just calls start() when it finished.
                // Since the old tasks will already be removed from the queue, this works.
                is Action.RepeatAmbiguous -> {
                    tasks.add(object : BukkitRunnable() { override fun run() {
                        if (abort) this.cancel()
                        if (!action.expression()) {
                            this.cancel()
                            offset = 0
                            start()
                        }
                        action.task()
                    }}.runTaskTimer(plugin, offset, action.delay.toLong()))
                    return this
                }

                // Repeating forever clears the entire action queue because there cannot
                // be anything after forever. Be careful with this one.
                is Action.RepeatForever -> {
                    actionQueue.clear()
                    tasks.add(object : BukkitRunnable() { override fun run() {
                        if (abort) this.cancel()
                        action.task()
                    }}.runTaskTimer(plugin, offset, action.delay.toLong()))
                    return this
                }
            }
        }
        return this
    }

    fun stop() {
        tasks.forEach {
            it.cancel()
        }
        tasks.clear()
        actionQueue.clear()
        actions.forEach { actionQueue.addLast(it) }
        offset = 0
    }

    fun destroy() {
        abort = true
        stop()
        crimson.runningEffects.remove(this)

    }
}
