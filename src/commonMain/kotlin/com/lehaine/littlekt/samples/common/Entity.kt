package com.lehaine.littlekt.samples.common

import com.lehaine.littlekt.math.interpolate
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 12/26/2021
 */
open class Entity(val gridCellSize: Int) {
    var cx: Int = 0
    var cy: Int = 0
    var xr: Float = 0.5f
    var yr: Float = 1f

    var gravityX: Float = 0f
    var gravityY: Float = 0f
    var gravityMultiplier: Float = 1f
    var velocityX: Float = 0f
    var velocityY: Float = 0f
    var frictionX: Float = 0.82f
    var frictionY: Float = 0.82f
    var maxGridMovementPercent: Float = 0.33f

    var width: Float = gridCellSize.toFloat()
    var height: Float = gridCellSize.toFloat()

    var anchorX: Float = 0.5f
    var anchorY: Float = 1f

    val innerRadius get() = min(width, height) * 0.5
    val outerRadius get() = max(width, height) * 0.5

    var interpolatePixelPosition: Boolean = false
    var lastPx: Float = 0f
    var lastPy: Float = 0f

    private var _stretchX = 1f
    private var _stretchY = 1f

    var stretchX: Float
        get() = _stretchX
        set(value) {
            _stretchX = value
            _stretchY = 2 - value
        }
    var stretchY: Float
        get() = _stretchY
        set(value) {
            _stretchX = 2 - value
            _stretchY = value
        }

    var scaleX = 1f
    var scaleY = 1f

    var restoreSpeed: Float = 12f

    var dir: Int = 1

    val px: Float
        get() {
            return if (interpolatePixelPosition) {
                fixedProgressionRatio.interpolate(lastPx, attachX)
            } else {
                attachX
            }
        }

    val py: Float
        get() {
            return if (interpolatePixelPosition) {
                fixedProgressionRatio.interpolate(lastPy, attachY)
            } else {
                attachY
            }
        }
    val attachX get() = (cx + xr) * gridCellSize
    val attachY get() = (cy + yr) * gridCellSize
    val centerX get() = attachX + (0.5f - anchorX) * gridCellSize
    val centerY get() = attachY + (0.5f - anchorY) * gridCellSize
    val top get() = attachY - anchorY * height
    val right get() = attachX + (1 - anchorX) * width
    val bottom get() = attachY + (1 - anchorY) * height
    val left get() = attachX - anchorX * width

    var fixedProgressionRatio: Float = 1f

    var preXCheck: (() -> Unit)? = null
    var preYCheck: (() -> Unit)? = null

    var destroyed = false
        protected set

    open fun update(dt: Duration) = Unit
    open fun fixedUpdate() {
        updateGridPosition()
    }

    open fun postUpdate(dt: Duration) = Unit

    open fun destroy() {
        if (destroyed) return
    }

    fun onPositionManuallyChanged() {
        lastPx = attachX
        lastPy = attachY
    }

    open fun updateGridPosition() {
        lastPx = attachX
        lastPy = attachY

        velocityX += calculateDeltaXGravity()
        velocityY += calculateDeltaYGravity()

        /**
         * Any movement greater than [maxGridMovementPercent] will increase the number of steps here.
         * The steps will break down the movement into smaller iterators to avoid jumping over grid collisions
         */
        var steps = ceil(abs(velocityX) + abs(velocityY) / maxGridMovementPercent)
        if (steps > 0) {
            val stepX = velocityX / steps
            val stepY = velocityY / steps
            while (steps > 0) {
                xr += stepX

                if (velocityX != 0f) {
                    preXCheck?.invoke()
                    checkXCollision()
                }

                while (xr > 1) {
                    xr--
                    cx++
                }
                while (xr < 0) {
                    xr++
                    cx--
                }

                yr += stepY


                if (velocityY != 0f) {
                    preYCheck?.invoke()
                    checkYCollision()
                }

                while (yr > 1) {
                    yr--
                    cy++
                }

                while (yr < 0) {
                    yr++
                    cy--
                }

                steps--
            }
        }
        velocityX *= frictionX
        if (abs(velocityX) <= 0.0005f) {
            velocityX = 0f
        }

        velocityY *= frictionY
        if (abs(velocityY) <= 0.0005f) {
            velocityY = 0f
        }
    }

    open fun calculateDeltaXGravity(): Float {
        return 0f
    }

    open fun calculateDeltaYGravity(): Float {
        return 0f
    }

    open fun checkXCollision() {}
    open fun checkYCollision() {}
}