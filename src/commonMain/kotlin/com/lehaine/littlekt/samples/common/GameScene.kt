package com.lehaine.littlekt.samples.common

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.Scene
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 12/26/2021
 */
open class GameScene(context: Context) : Scene(context) {
    val updateComponents = mutableListOf<UpdateComponent>()

    override fun render(dt: Duration) {
        updateComponents.forEach {
            it.update(dt)
        }
    }
}

fun <T : GameScene> T.addFixedInterpUpdater(
    timesPerSecond: Float,
    initial: Boolean = true,
    interpolate: (ratio: Float) -> Unit,
    updatable: Scene.() -> Unit
) = (this as Scene).addFixedInterpUpdater(timesPerSecond, initial, interpolate, updatable).also { updateComponents += it }

fun <T : GameScene> T.addFixedInterpUpdater(
    time: Duration,
    initial: Boolean = true,
    interpolate: (ratio: Float) -> Unit,
    updatable: Scene.() -> Unit
) = (this as Scene).addFixedInterpUpdater(time, initial, interpolate, updatable).also { updateComponents += it }


fun <T : GameScene> T.addTmodUpdater(
    targetFPS: Int,
    updatable: Scene.(dt: Duration, tmod: Float) -> Unit
) = (this as Scene).addTmodUpdater(targetFPS, updatable).also { updateComponents += it }