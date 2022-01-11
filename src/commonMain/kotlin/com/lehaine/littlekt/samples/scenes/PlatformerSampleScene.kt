package com.lehaine.littlekt.samples.scenes

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.audio.AudioClip
import com.lehaine.littlekt.graph.node.component.HAlign
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.font.BitmapFont
import com.lehaine.littlekt.graphics.font.BitmapFontCache
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkEntity
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkLevel
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkWorld
import com.lehaine.littlekt.input.Input
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.samples.common.*
import com.lehaine.littlekt.util.fastForEach
import com.lehaine.littlekt.util.viewport.ExtendViewport
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * @author Colton Daily
 * @date 12/24/2021
 */
class PlatformerSampleScene(
    val batch: SpriteBatch,
    val font: BitmapFont,
    context: Context
) : GameScene(context) {
    private val fontCache = BitmapFontCache(font)
    private val entities = mutableListOf<Entity>()
    private val atlas: TextureAtlas by load(resourcesVfs["tiles.atlas.json"])

    private val sfxFootstep: AudioClip by load(resourcesVfs["sfx/footstep0.wav"])
    private val sfxLand: AudioClip by load(resourcesVfs["sfx/land0.wav"])
    private val sfxPickup: AudioClip by load(resourcesVfs["sfx/pickup0.wav"])

    private val world: LDtkWorld by load(resourcesVfs["platformer.ldtk"])
    private val ldtkLevel: LDtkLevel by prepare { world.levels[0] }
    private val level: PlatformerLevel by prepare { PlatformerLevel(ldtkLevel) }
    private val fx by prepare { Fx(atlas) }

    private val hero: Hero by prepare {
        Hero(
            ldtkLevel.entities("Player")[0],
            sfxFootstep,
            sfxLand,
            atlas,
            level,
            camera,
            fx,
            input
        ).also {
            it.onDestroy = ::removeEntity
        }
    }

    private val camera = GameCamera(virtualWidth = graphics.width, virtualHeight = graphics.height).apply {
        viewport = ExtendViewport(200, 200)
    }
    private val uiCam = OrthographicCamera(graphics.width, graphics.height).apply {
        viewport = ExtendViewport(480, 270)
    }
    private val gameOver get() = Diamond.ALL.size == 0
    private var fixedProgressionRatio = 1f

    override fun create() {
        initLevel()

        addTmodUpdater(60) { dt, tmod ->
            if (input.isKeyJustPressed(Key.R) && gameOver) {
                entities.fastForEach {
                    it.destroy()
                }
                entities.clear()
                initLevel()
            }

            fx.update(dt, tmod)

            entities.fastForEach {
                it.fixedProgressionRatio = fixedProgressionRatio
                it.update(dt)
            }

            entities.fastForEach {
                it.postUpdate(dt)
            }

            camera.update(dt)
            camera.viewport.apply(this)
            batch.use(camera.viewProjection) {
                ldtkLevel.render(it, camera)
                fx.render(it)
                entities.fastForEach { entity ->
                    entity.render(it)
                }
                hero.render(it)
            }
            uiCam.update()
            uiCam.viewport.apply(this)

            batch.use(uiCam.viewProjection) {
                fontCache.draw(it)
            }
        }

        addFixedInterpUpdater(
            30f,
            interpolate = { ratio -> fixedProgressionRatio = ratio },
            updatable = { entities.fastForEach { it.fixedUpdate() } }
        )
    }

    private fun initLevel() {
        hero.setFromLevelEntity(ldtkLevel.entities("Player")[0])
        entities += hero
        ldtkLevel.entities("Diamond").forEach { ldtkEntity ->
            entities += Diamond(ldtkEntity, sfxPickup, atlas, level, hero).also {
                entities += it
                it.onDestroy = ::removeEntity
            }
        }
        camera.viewBounds.width = ldtkLevel.pxWidth.toFloat()
        camera.viewBounds.height = ldtkLevel.pxHeight.toFloat()
        camera.follow(hero, true)
        fontCache.setText("Diamonds left: ${Diamond.ALL.size}", 10f, 5f, scaleX = 2f, scaleY = 2f)
    }

    private fun removeEntity(entity: Entity) {
        entities.remove(entity)
        if (entity is Diamond) {
            fontCache.setText("Diamonds left: ${Diamond.ALL.size}", 10f, 5f, scaleX = 2f, scaleY = 2f)
        }
        if (gameOver) {
            fontCache.setText(
                "You Win!\nR to Restart",
                uiCam.virtualWidth * 0.5f,
                uiCam.virtualHeight * 0.5f - 30,
                scaleX = 2f,
                scaleY = 2f,
                align = HAlign.CENTER
            )
        }
    }

    override suspend fun resize(width: Int, height: Int) {
        camera.update(width, height, this)
        uiCam.update(width, height, this)
    }

    override fun dispose() {
        world.dispose()
        atlas.entries.forEach {
            it.texture.dispose()
        }
        sfxFootstep.dispose()
        sfxLand.dispose()
        sfxPickup.dispose()
    }
}

class PlatformerLevel(level: LDtkLevel) : LDtkGameLevel<PlatformerLevel.LevelMark>(level) {
    override var gridSize: Int = 8

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
    private val sfxFootstep: AudioClip,
    private val sfxLand: AudioClip,
    private val atlas: TextureAtlas,
    override val level: PlatformerLevel,
    private val camera: GameCamera,
    private val fx: Fx,
    private val input: Input
) : PlatformEntity(level, level.gridSize) {
    private val idle = atlas.getAnimation("heroIdle", 500.milliseconds)
    private val run = atlas.getAnimation("heroRun")

    private val speed = 0.08f
    private var moveDir = 0f
    private val jumpHeight = -1.35f
    private var lastHeight = py
    private var jumping = false

    init {
        useTopCollisionRatio = true
        topCollisionRatio = 0.5f
        setFromLevelEntity(data)
    }

    override fun update(dt: Duration) {
        super.update(dt)
        moveDir = 0f

        if (onGround) {
            cd(ON_GROUND_RECENTLY, 150.milliseconds)
            if (py - lastHeight > 25) {
                sfxLand.play()
                camera.shake(25.milliseconds, 0.7f)
            }
            lastHeight = py
        } else {
            if (velocityY < 0) {
                lastHeight = py
            }
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
            if (onGround && !cd.has(RUN_DUST)) {
                cd.timeout(RUN_DUST, 100.milliseconds)
                fx.runDust(centerX, bottom, -dir)
            }
            if (onGround && !cd.has(FOOTSTEP)) {
                sfxFootstep.play(0.30f)
                cd.timeout(FOOTSTEP, 350.milliseconds)
            }
            dir = if (input.isKeyPressed(Key.D)) 1 else -1
            moveDir = dir.toFloat()
            anim.playLooped(run)
        } else {
            anim.playLooped(idle)
        }
    }

    private fun jump() {
        if (input.isKeyJustPressed(Key.SPACE) && cd.has(ON_GROUND_RECENTLY)) {
            velocityY = jumpHeight
            stretchX = 0.7f
            jumping = true
        }
        if (!input.isKeyPressed(Key.SPACE) && jumping) {
            velocityY *= 0.5f
            jumping = false
        }
    }

    companion object {
        private const val ON_GROUND_RECENTLY = "onGroundRecently"
        private const val FOOTSTEP = "footstep"
        private const val RUN_DUST = "runDust"
    }
}

class Diamond(
    data: LDtkEntity,
    private val sfxPickup: AudioClip,
    private val atlas: TextureAtlas,
    level: PlatformerLevel,
    private val hero: Hero
) : LevelEntity(level, level.gridSize) {

    init {
        sprite = atlas.getByPrefix("diamond").slice
        setFromLevelEntity(data)
        ALL += this
    }

    override fun update(dt: Duration) {
        if (hero.isCollidingWith(this)) {
            sfxPickup.play()
            destroy()
        }
    }

    override fun destroy() {
        ALL.remove(this)
        super.destroy()
    }

    companion object {
        val ALL = mutableListOf<Diamond>()
    }
}