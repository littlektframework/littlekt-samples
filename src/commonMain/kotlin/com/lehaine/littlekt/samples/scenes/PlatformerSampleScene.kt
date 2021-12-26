package com.lehaine.littlekt.samples.scenes

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.font.GpuFont
import com.lehaine.littlekt.graphics.font.use
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkEntity
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkLevel
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkTileMap
import com.lehaine.littlekt.samples.common.*
import com.lehaine.littlekt.util.viewport.FitViewport

/**
 * @author Colton Daily
 * @date 12/24/2021
 */
class PlatformerSampleScene(
    val batch: SpriteBatch,
    val gpuFontRenderer: GpuFont,
    context: Context
) : GameScene(context) {
    private val entities = mutableListOf<Entity>()
    private val atlas: TextureAtlas by load(resourcesVfs["tiles.atlas.json"])
    private val world: LDtkTileMap by load(resourcesVfs["platformer.ldtk"])
    private val ldtkLevel: LDtkLevel by prepare { world.levels[0] }
    private val level: PlatformerLevel by prepare { PlatformerLevel(ldtkLevel) }
    private val hero: Hero by prepare { Hero(ldtkLevel.entities("Player")[0], atlas, level) }
    private val diamonds: List<LDtkEntity> by prepare { ldtkLevel.entities("Diamond") }

    private val camera = OrthographicCamera(graphics.width, graphics.height).apply {
        viewport = FitViewport(480, 270)
    }

    private var fixedProgressionRatio = 1f

    override fun create() {
        camera.translate(ldtkLevel.pxWidth / 2f, ldtkLevel.pxHeight / 2f, 0f)

        addTmodUpdater(60) { dt, tmod ->
            entities.forEach {
                it.update(dt)
            }
            entities.forEach {
                it.postUpdate(dt)
            }

            camera.update()
            batch.use(camera.viewProjection) {
                ldtkLevel.render(it, camera)
                hero.render(it)
            }
            gpuFontRenderer.use(camera.viewProjection) {
                it.drawText("Diamonds left: ${diamonds.size}", 10f, 10f, 36, color = Color.WHITE)
            }
        }

        addFixedInterpUpdater(
            30f,
            interpolate = { ratio -> fixedProgressionRatio = ratio },
            updatable = { entities.forEach { it.fixedUpdate() } }
        )
    }

    override fun resize(width: Int, height: Int) {
        camera.viewport.update(width, height, this)
    }

    override fun dispose() {
        world.dispose()
    }
}

class PlatformerLevel(level: LDtkLevel) : LDtkGameLevel<PlatformerLevel.LevelMark>(level) {
    override var gridSize: Int = 16

    init {
        createLevelMarks()
    }

    // set level marks at start of level creation to react to certain tiles
    override fun createLevelMarks() {
        for (cy in 0 until levelHeight) {
            for (cx in 0 until levelWidth) {
                // no collision at current pos or north but has collision south.
                if (!hasCollision(cx, cy) && hasCollision(cx, cy + 1) && !hasCollision(cx, cy - 1)) {
                    // if collision to the east of current pos and no collision to the northeast
                    if (hasCollision(cx + 1, cy) && !hasCollision(cx + 1, cy - 1)) {
                        setMark(cx, cy, LevelMark.SMALL_STEP, 1);
                    }

                    // if collision to the west of current pos and no collision to the northwest
                    if (hasCollision(cx - 1, cy) && !hasCollision(cx - 1, cy - 1)) {
                        setMark(cx, cy, LevelMark.SMALL_STEP, -1);
                    }
                }

                if (!hasCollision(cx, cy) && hasCollision(cx, cy + 1)) {
                    if (hasCollision(cx + 1, cy) ||
                        (!hasCollision(cx + 1, cy + 1) && !hasCollision(cx + 1, cy + 2))
                    ) {
                        setMarks(cx, cy, listOf(LevelMark.PLATFORM_END, LevelMark.PLATFORM_END_RIGHT))
                    }
                    if (hasCollision(cx - 1, cy) ||
                        (!hasCollision(cx - 1, cy + 1) && !hasCollision(cx - 1, cy + 2))
                    ) {
                        setMarks(cx, cy, listOf(LevelMark.PLATFORM_END, LevelMark.PLATFORM_END_LEFT))
                    }
                }
            }
        }
    }

    enum class LevelMark {
        PLATFORM_END,
        PLATFORM_END_RIGHT,
        PLATFORM_END_LEFT,
        SMALL_STEP
    }
}

class Hero(
    data: LDtkEntity,
    private val atlas: TextureAtlas,
    override val level: PlatformerLevel
) :
    PlatformEntity(level, level.gridSize) {
    val sprite: TextureSlice = atlas["heroIdle0.png"].slice

    var x: Float = data.x
    var y: Float = data.y
    var pivotX = data.pivotX
    var pivotY = data.pivotY

    fun render(batch: SpriteBatch) {
        batch.draw(sprite, x, y, sprite.width * pivotX, sprite.height * pivotY)
    }
}