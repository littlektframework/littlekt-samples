package com.littlekt.samples.game.common

import com.littlekt.Context
import com.littlekt.Releasable
import kotlin.time.Duration

/**
 * A base scene. Does nothing on its own and will need to be managed by either a [ContextListener] or [Game].
 * @see Game
 * @see ContextListener
 * @author Colton Daily
 * @date 12/26/2021
 */
abstract class Scene(val context: Context) : Releasable {
    /**
     * Invoked when a [Scene] is first shown.
     */
    open suspend fun Context.show() = Unit

    /**
     * Invoked on every render frame.
     */
    open fun Context.update(dt: Duration) = Unit

    /**
     * Invoked when a resize event occurs.
     */
    open fun Context.resize(width: Int, height: Int) = Unit

    /**
     * Invoked when this scene is hidden from view.
     */
    open suspend fun Context.hide() = Unit

    final override fun release() {
        context.release()
    }

    /**
     * Dispose of any assets here.
     */
    open fun Context.release() = Unit
}