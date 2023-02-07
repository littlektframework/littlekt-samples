package com.lehaine.littlekt.samples.game.common

import com.lehaine.littlekt.graphics.g2d.tilemap.ldtk.LDtkIntGridLayer
import com.lehaine.littlekt.graphics.g2d.tilemap.ldtk.LDtkLevel
import com.lehaine.littlekt.math.clamp

/**
 * @author Colton Daily
 * @date 12/26/2021
 */
abstract class LDtkGameLevel<LevelMark>(var level: LDtkLevel) : GameLevel<LevelMark> {
    val levelWidth get() = level["Collisions"].gridWidth
    val levelHeight get() = level["Collisions"].gridHeight

    protected val marks = mutableMapOf<LevelMark, MutableMap<Int, Int>>()

    // a list of collision layers indices from LDtk world
    protected val collisionLayers = intArrayOf(1)
    protected val collisionLayer = level["Collisions"] as LDtkIntGridLayer

    override fun isValid(cx: Int, cy: Int) = collisionLayer.isCoordValid(cx, cy)
    override fun getCoordId(cx: Int, cy: Int) = collisionLayer.getCoordId(cx, cy)

    override fun hasCollision(cx: Int, cy: Int): Boolean {
        return if (isValid(cx, cy)) {
            collisionLayers.contains(collisionLayer.getInt(cx, cy))
        } else {
            true
        }
    }

    override fun hasMark(cx: Int, cy: Int, mark: LevelMark, dir: Int): Boolean {
        return marks[mark]?.get(getCoordId(cx, cy)) == dir && isValid(cx, cy)
    }

    override fun setMarks(cx: Int, cy: Int, marks: List<LevelMark>) {
        marks.forEach {
            setMark(cx, cy, it)
        }
    }

    override fun setMark(cx: Int, cy: Int, mark: LevelMark, dir: Int) {
        if (isValid(cx, cy) && !hasMark(cx, cy, mark)) {
            if (!marks.contains(mark)) {
                marks[mark] = mutableMapOf()
            }

            marks[mark]?.set(getCoordId(cx, cy), dir.clamp(-1, 1))
        }
    }

    // set level marks at start of level creation to react to certain tiles
    protected open fun createLevelMarks() = Unit

}