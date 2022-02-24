package com.lehaine.littlekt.samples.common

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.Scene
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 12/26/2021
 */
open class GameScene(context: Context) : Scene(context) {
    val updateComponents = mutableListOf<UpdateComponent>()

    override suspend fun Context.render(dt: Duration) {
        gl.clear(ClearBufferMask.COLOR_DEPTH_BUFFER_BIT)
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