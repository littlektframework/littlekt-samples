package com.lehaine.littlekt.samples.game.common

import com.lehaine.littlekt.graphics.OrthographicCamera
import com.lehaine.littlekt.math.Rect
import com.lehaine.littlekt.math.clamp
import com.lehaine.littlekt.math.dist
import com.lehaine.littlekt.math.geom.Angle
import com.lehaine.littlekt.math.geom.cosine
import com.lehaine.littlekt.math.interpolate
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 12/27/2021
 */
class GameCamera(
    val viewBounds: Rect = Rect(),
    val snapToPixel: Boolean = true,
    virtualWidth: Int,
    virtualHeight: Int
) : OrthographicCamera(virtualWidth, virtualHeight) {
    var deadZone: Int = 5
    var clampToBounds = true
    var following: Entity? = null
        private set

    private var shakePower = 1f
    private var shakeFrames = 0

    private var bumpX = 0f
    private var bumpY = 0f

    private val cd = CooldownComponent()

    fun update(dt: Duration) {
        cd.update(dt)

        val following = following
        if (following != null) {
            val dist = dist(position.x, position.y, following.px, following.py)
            if (dist >= deadZone) {
                val speedX = 0.015f * zoom
                val speedY = 0.023 * zoom
                position.x = speedX.interpolate(position.x, following.px)
                position.y = speedY.interpolate(position.y, following.py)
            }
        }

        if (clampToBounds) {
            position.x = if (viewBounds.width < virtualWidth) {
                viewBounds.width * 0.5f
            } else {
                position.x.clamp(virtualWidth * 0.5f, viewBounds.width - virtualWidth * 0.5f)
            }

            position.y = if (viewBounds.height < virtualHeight) {
                viewBounds.height * 0.5f
            } else {
                position.y.clamp(virtualHeight * 0.5f, viewBounds.height - virtualHeight * 0.5f)
            }

        }
        bumpX *= 0.75f
        bumpY *= 0.75f

        position.x += bumpX
        position.y += bumpY

        if (cd.has(SHAKE)) {
            position.x += cos(shakeFrames * 1.1f) * 2.5f * shakePower * cd.ratio(SHAKE)
            position.y += sin(0.3f + shakeFrames * 1.7f) * 2.5f * shakePower * cd.ratio(SHAKE)
            shakeFrames++
        } else {
            shakeFrames = 0
        }

        if (snapToPixel) {
            position.x = position.x.roundToInt().toFloat()
            position.y = position.y.roundToInt().toFloat()
        }

        update()
    }

    fun shake(time: Duration, power: Float = 1f) {
        cd.timeout(SHAKE, time)
        shakePower = power
    }

    fun bump(x: Float = 0f, y: Float = 0f) {
        bumpX += x
        bumpY += y
    }

    fun bump(x: Int = 0, y: Int = 0) = bump(x.toFloat(), y.toFloat())

    fun bump(angle: Angle, distance: Int) {
        bumpX += angle.cosine * distance
        bumpY += angle.radians * distance
    }

    fun follow(entity: Entity?, setImmediately: Boolean = false) {
        following = entity
        if (setImmediately) {
            entity ?: error("Target entity not set!!")
            position.set(entity.px, entity.py, 0f)
        }
    }

    fun unfollow() {
        following = null
    }

    companion object {
        private const val SHAKE = "shake"
    }
}