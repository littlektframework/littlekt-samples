package com.lehaine.littlekt.samples.game.common

import com.lehaine.littlekt.util.datastructure.Pool
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * @author Colton Daily
 * @date 12/26/2021
 */
private data class CooldownTimer(
    var time: Duration,
    var name: String,
    var callback: () -> Unit
) {
    val ratio get() = 1f - (elapsed / time).toFloat()
    var elapsed = 0.milliseconds
    val finished get() = elapsed >= time

    fun update(dt: Duration) {
        elapsed += dt
        if (finished) {
            callback()
        }
    }
}

/**
 * @author Colton Daily
 * @date 12/26/2021
 */
class CooldownComponent : UpdateComponent {
    private val cooldownTimerPool = Pool(
        reset = {
            it.elapsed = 0.milliseconds
            it.time = 0.milliseconds
            it.name = ""
            it.callback = {}
        },
        gen = { CooldownTimer(0.milliseconds, "", {}) })

    private val timers = mutableMapOf<String, CooldownTimer>()

    override fun update(dt: Duration) {
        val iterate = timers.iterator()
        while (iterate.hasNext()) {
            val timer = iterate.next().value.also { it.update(dt) }
            if (timer.finished) {
                iterate.remove()
            }
        }
    }

    private fun addTimer(name: String, timer: CooldownTimer) {
        timers[name] = timer
    }

    private fun removeTimer(name: String) {
        timers.remove(name)?.also {
            cooldownTimerPool.free(it)
        }
    }

    private fun reset(name: String, time: Duration, callback: () -> Unit) {
        timers[name]?.apply {
            this.time = time
            this.callback = callback
            this.elapsed = 0.milliseconds
        }
    }

    private fun interval(name: String, time: Duration, callback: () -> Unit = {}) {
        if (has(name)) {
            reset(name, time, callback)
            return
        }
        val timer = cooldownTimerPool.alloc().apply {
            this.time = time
            this.name = name
            this.callback = callback
        }
        addTimer(name, timer)
    }


    fun timeout(name: String, time: Duration, callback: () -> Unit = { }) =
        interval(name, time, callback)

    fun has(name: String) = timers[name] != null

    fun remove(name: String) = removeTimer(name)

    fun ratio(name: String): Float {
        return timers[name]?.ratio ?: 0f
    }
}