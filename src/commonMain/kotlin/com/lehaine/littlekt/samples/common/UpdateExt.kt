package com.lehaine.littlekt.samples.common

import com.lehaine.littlekt.Scene
import com.lehaine.littlekt.util.milliseconds
import com.lehaine.littlekt.util.seconds
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * @author Colton Daily
 * @date 12/26/2021
 */

interface UpdateComponent {
    fun update(dt: Duration) = Unit
}

fun <T : Scene> T.addFixedInterpUpdater(
    timesPerSecond: Float,
    initial: Boolean = true,
    interpolate: (ratio: Float) -> Unit,
    updatable: T.() -> Unit
): UpdateComponent = addFixedInterpUpdater(
    (1f / timesPerSecond).toDouble().seconds,
    initial,
    interpolate,
    updatable
)

fun <T : Scene> T.addFixedInterpUpdater(
    time: Duration,
    initial: Boolean = true,
    interpolate: (ratio: Float) -> Unit,
    updatable: T.() -> Unit
): UpdateComponent {
    var accum = 0.milliseconds
    val component = object : UpdateComponent {
        override fun update(dt: Duration) {
            accum += dt
            while (accum >= time * 0.75) {
                accum -= time
                updatable(this@addFixedInterpUpdater)
            }

            interpolate(accum.milliseconds / time.milliseconds)
        }
    }
    if (initial) {
        updatable(this@addFixedInterpUpdater)
    }
    return component
}

fun <T : Scene> T.addTmodUpdater(targetFPS: Int, updatable: T.(dt: Duration, tmod: Float) -> Unit): UpdateComponent {
    val component = object : UpdateComponent {
        override fun update(dt: Duration) {
            updatable(this@addTmodUpdater, dt, dt.seconds * targetFPS)
        }
    }
    component.update(0.0.seconds)
    return component
}