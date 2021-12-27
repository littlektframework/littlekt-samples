package com.lehaine.littlekt.samples.scenes

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.font.GpuFont
import com.lehaine.littlekt.graphics.font.use
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkEntity
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkLevel
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkTileMap
import com.lehaine.littlekt.input.Input
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.samples.common.*
import com.lehaine.littlekt.util.viewport.ExtendViewport
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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
    private val hero: Hero by prepare {
        Hero(
            ldtkLevel.entities("Player")[0],
            atlas,
            level,
            input
        ).also { entities += it }
    }
    private val diamonds: List<LDtkEntity> by prepare { ldtkLevel.entities("Diamond") }

    private val camera = OrthographicCamera(graphics.width, graphics.height).apply {
        viewport = ExtendViewport(480, 270)
    }
    private val uiCam = OrthographicCamera(graphics.width, graphics.height).apply {
        viewport = ExtendViewport(480, 270)
    }

    private var fixedProgressionRatio = 1f

    override fun create() {
        camera.position.set(ldtkLevel.pxWidth / 2f, ldtkLevel.pxHeight / 2f, 0f)

        addTmodUpdater(60) { dt, tmod ->
            entities.forEach {
                it.fixedProgressionRatio = fixedProgressionRatio
                it.update(dt)
            }
            entities.forEach {
                it.postUpdate(dt)
            }

            camera.update()
            camera.viewport.apply(this)
            batch.use(camera.viewProjection) {
                ldtkLevel.render(it, camera)
                hero.render(it)
            }
            uiCam.update()
            uiCam.viewport.apply(this)
            gpuFontRenderer.use(uiCam.viewProjection) {
                it.drawText("Diamonds left: ${diamonds.size}", 10f, 25f, 36, color = Color.WHITE)
            }
        }

        addFixedInterpUpdater(
            30f,
            interpolate = { ratio -> fixedProgressionRatio = ratio },
            updatable = { entities.forEach { it.fixedUpdate() } }
        )
    }

    override fun resize(width: Int, height: Int) {
        camera.update(width, height, this)
        uiCam.update(width, height, this)
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
    override val level: PlatformerLevel,
    val input: Input
) : PlatformEntity(level, level.gridSize) {
    val sprite: TextureSlice = atlas["heroIdle0.png"].slice

    private val speed = 0.08f
    private var moveDir = 0f
    private val jumpHeight = -0.65f

    init {
        width = 8f
        height = 8f
        cx = data.cx
        cy = data.cy
        xr = data.pivotX
        yr = data.pivotY
        anchorX = data.pivotX
        anchorY = data.pivotY
    }

    fun render(batch: SpriteBatch) {
        batch.draw(
            slice = sprite,
            x = px,
            y = py,
            originX = sprite.width * anchorX,
            originY = sprite.height * anchorY,
            scaleX = scaleX,
            scaleY = scaleY
        )
    }

    override fun update(dt: Duration) {
        super.update(dt)
        moveDir = 0f

        if (onGround) {
            cd(ON_GROUND_RECENTLY, 150.milliseconds)
        }

        run()
        jump()
    }

    override fun fixedUpdate() {
        super.fixedUpdate()
        if (moveDir != 0f) {
            velocityX += moveDir * speed
        } else {
            velocityX *= 0.3f
        }
    }

    private fun run() {
        if (input.isKeyPressed(Key.A) || input.isKeyPressed(Key.D)) {
            dir = if (input.isKeyPressed(Key.D)) 1 else -1
            moveDir = dir.toFloat()
        }
    }

    private fun jump() {
        if (input.isKeyJustPressed(Key.SPACE) && cd.has(ON_GROUND_RECENTLY)) {
            velocityY = jumpHeight
            stretchX = 0.7f
        }
    }

    companion object {
        private const val ON_GROUND_RECENTLY = "onGroundRecently"
    }
}