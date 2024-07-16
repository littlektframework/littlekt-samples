package com.littlekt.samples.game.common

import com.littlekt.Context
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 12/26/2021
 */
open class GameScene(context: Context) : Scene(context) {
    val updateComponents = mutableListOf<UpdateComponent>()

    override fun Context.update(dt: Duration) {
        updateComponents.forEach {
            it.update(dt)
        }
    }
}

fun <T : GameScene> T.addFixedInterpUpdater(
    timesPerSecond: Float,
    initial: Boolean = true,
    interpolate: (ratio: Float) -> Unit,
    updatable: T.() -> Unit
) = createFixedInterpUpdater(timesPerSecond, initial, interpolate, updatable).also { updateComponents += it }

fun <T : GameScene> T.addFixedInterpUpdater(
    time: Duration,
    initial: Boolean = true,
    interpolate: (ratio: Float) -> Unit,
    updatable: T.() -> Unit
) = createFixedInterpUpdater(time, initial, interpolate, updatable).also { updateComponents += it }

fun <T : GameScene> T.addTmodUpdater(
    targetFPS: Int,
    updatable: T.(dt: Duration, tmod: Float) -> Unit
) = createTmodUpdater(targetFPS, updatable).also { updateComponents += it }