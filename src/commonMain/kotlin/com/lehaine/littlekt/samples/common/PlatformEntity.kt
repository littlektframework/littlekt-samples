package com.lehaine.littlekt.samples.common

/**
 * @author Colton Daily
 * @date 12/26/2021
 */
open class PlatformEntity(level: GameLevel<*>, gridCellSize: Int) : LevelEntity(level, gridCellSize) {
    val onGround
        get() = velocityY == 0f && level.hasCollision(
            cx,
            cy + 1
        ) && yr == bottomCollisionRatio

    var hasGravity: Boolean = false


    private val gravityPulling get() = !onGround && hasGravity


    override fun calculateDeltaYGravity(): Float {
        return if (gravityPulling) {
            gravityMultiplier * gravityY
        } else {
            0f
        }
    }
}