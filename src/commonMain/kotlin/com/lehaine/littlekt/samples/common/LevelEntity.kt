package com.lehaine.littlekt.samples.common

import kotlin.math.floor

/**
 * @author Colton Daily
 * @date 12/26/2021
 */
open class LevelEntity(
    open val level: GameLevel<*>,
    gridCellSize: Int
) : Entity(gridCellSize) {
    var rightCollisionRatio: Float = 0.7f
    var leftCollisionRatio: Float = 0.3f
    var bottomCollisionRatio: Float = 1f
    var topCollisionRatio: Float = 1f
    var useTopCollisionRatio: Boolean = false

    override fun checkXCollision() {
        if (level.hasCollision(cx + 1, cy) && xr >= rightCollisionRatio) {
            xr = rightCollisionRatio
            velocityX *= 0.5f
            onLevelCollision(1, 0)
        }

        if (level.hasCollision(cx - 1, cy) && xr <= leftCollisionRatio) {
            xr = leftCollisionRatio
            velocityX *= 0.5f
            onLevelCollision(-1, 0)
        }
    }

    override fun checkYCollision() {
        val heightCoordDiff = if (useTopCollisionRatio) topCollisionRatio else floor(height / gridCellSize.toFloat())
        if (level.hasCollision(cx, cy - 1) && yr <= heightCoordDiff) {
            yr = heightCoordDiff
            velocityY = 0f
            onLevelCollision(0, -1)
        }
        if (level.hasCollision(cx, cy + 1) && yr >= bottomCollisionRatio) {
            velocityY = 0f
            yr = bottomCollisionRatio
            onLevelCollision(0, 1)
        }
    }

    open fun onLevelCollision(xDir: Int, yDir: Int) = Unit
}