package de.zohiu.crimson

import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.util.ArrayDeque


/**
 * Effect builder is still considered experimental
 * Breaking changes are very likely
 */
class Effect(val crimson: Crimson, val plugin: JavaPlugin) {
    private val actionQueue = ArrayDeque<Action>()
    private val tasks: MutableList<BukkitTask> = mutableListOf()
    private var abort = false
    private var timeOffsetTicks: Long = 0

    private fun bakeTask(task: () -> Unit, timeOffsetTicks: Long): BukkitTask {
        return object : BukkitRunnable() { override fun run() {
            if (abort) return
            task()
        }}.runTaskLater(plugin, timeOffsetTicks)
    }

    fun run(task: () -> Unit): Effect {
        val action = Action(ActionType.TASK)
        action.task = task
        actionQueue.addLast(action)
        return this
    }

    fun repeat(amount: Int, delay: Int, task: () -> Unit): Effect {
        val action = Action(ActionType.REPEAT)
        action.task = task
        action.amount = amount
        action.delay = delay
        actionQueue.addLast(action)
        return this
    }

    fun repeatUntil(expression: () -> Boolean, every: Int, task: () -> Unit): Effect {
        val action = Action(ActionType.REPEAT_UNTIL)
        action.expression = expression
        action.task = task
        action.delay = every
        actionQueue.addLast(action)
        return this
    }

    fun repeatForever(every: Int, task: () -> Unit): Effect {
        val action = Action(ActionType.REPEAT_FOREVER)
        action.task = task
        action.delay = every
        actionQueue.addLast(action)
        return this
    }

    fun wait(ticks: Int): Effect {
        val action = Action(ActionType.DELAY)
        action.delay = ticks
        actionQueue.addLast(action)
        return this
    }

    fun start() {
        if (!crimson.runningEffects.contains(this)) {
            crimson.runningEffects.add(this)
        }

        while (actionQueue.size > 0) {
            if (abort) return
            val currentAction = actionQueue.pollFirst() // ?: break

            if (currentAction.type == ActionType.DELAY) {
                timeOffsetTicks += currentAction.delay
                continue
            }

            if (currentAction.type == ActionType.REPEAT) {
                repeat(currentAction.amount) {
                    tasks.add(bakeTask(currentAction.task, timeOffsetTicks))
                    timeOffsetTicks += currentAction.delay
                }
                continue
            }

            // This one can't be baked because it takes an undefined amount of time
            // That's why it breaks the loop and just calls start() when it finished.
            // Since the old tasks will already be removed from the queue, this works.
            if (currentAction.type == ActionType.REPEAT_UNTIL) {
                tasks.add(object : BukkitRunnable() { override fun run() {
                    if (abort) this.cancel()
                    if (currentAction.expression()) {
                        this.cancel()
                        timeOffsetTicks = 0
                        start()
                    }
                    currentAction.task()
                }}.runTaskTimer(plugin, timeOffsetTicks, currentAction.delay.toLong()))
                return
            }

            // Repeating forever clears the entire action queue because there cannot
            // be anything after forever. Be careful with this one.
            if (currentAction.type == ActionType.REPEAT_FOREVER) {
                actionQueue.clear()
                tasks.add(object : BukkitRunnable() { override fun run() {
                    if (abort) this.cancel()
                    currentAction.task()
                }}.runTaskTimer(plugin, timeOffsetTicks, currentAction.delay.toLong()))
                return
            }

            tasks.add(bakeTask(currentAction.task, timeOffsetTicks))
        }
    }

    fun abort() {
        abort = true
        tasks.forEach {
            it.cancel()
        }
        crimson.runningEffects.remove(this)
    }
}

enum class ActionType {
    TASK, DELAY, REPEAT, REPEAT_UNTIL, REPEAT_FOREVER
}

class Action(val type: ActionType) {
    var expression: () -> Boolean = { true }
    var task: () -> Unit = {}
    var amount: Int = 1
    var delay: Int = 0
}