package com.lehaine.littlekt.samples.common

import com.lehaine.littlekt.graphics.OrthographicCamera
import com.lehaine.littlekt.math.Rect
import com.lehaine.littlekt.math.clamp
import com.lehaine.littlekt.math.dist
import com.lehaine.littlekt.math.interpolate
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 12/27/2021
 */
class GameCamera(
    val viewBounds: Rect = Rect(),
    virtualWidth: Int,
    virtualHeight: Int
) :
    OrthographicCamera(virtualWidth, virtualHeight) {
    var deadZone: Int = 5
    var clampToBounds = true
    var following: Entity? = null
        private set

    private var shakePower = 1f
    private val cd = CooldownComponent()

    fun update(dt: Duration) {
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
        update()
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
}