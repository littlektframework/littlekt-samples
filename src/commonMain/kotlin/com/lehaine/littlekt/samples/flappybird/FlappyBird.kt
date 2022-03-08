package com.lehaine.littlekt.samples.flappybird

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.file.vfs.readAtlas
import com.lehaine.littlekt.file.vfs.readBitmapFont
import com.lehaine.littlekt.graph.node.component.HAlign
import com.lehaine.littlekt.graph.node.node2d.ui.TextureRect
import com.lehaine.littlekt.graph.node.node2d.ui.label
import com.lehaine.littlekt.graph.node.node2d.ui.textureRect
import com.lehaine.littlekt.graph.sceneGraph
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.input.Pointer
import com.lehaine.littlekt.math.MutableVec2f
import com.lehaine.littlekt.math.Rect
import com.lehaine.littlekt.util.calculateViewBounds
import com.lehaine.littlekt.util.milliseconds
import com.lehaine.littlekt.util.viewport.ExtendViewport
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 3/4/2022
 */
class FlappyBird(context: Context) : ContextListener(context) {

    private var started = false
    private var gameOver = false
    private var score = 0

    override suspend fun Context.start() {
        val atlas = resourcesVfs["tiles.atlas.json"].readAtlas()
        val pixelFont =
            resourcesVfs["m5x7_16_outline.fnt"].readBitmapFont(preloadedTextures = listOf(atlas["m5x7_16_outline_0"].slice))
        val pipeHead = atlas.getByPrefix("pipeHead").slice
        val pipeBody = atlas.getByPrefix("pipeBody").slice

        val batch = SpriteBatch(this)
        val gameCamera = OrthographicCamera(graphics.width, graphics.height).apply {
            viewport = ExtendViewport(135, 256)
        }
        val viewBounds = Rect()
        val ui = sceneGraph(this, ExtendViewport(135, 256)) {
            label {
                anchorRight = 1f
                anchorTop = 0.10f
                text = "0"
                font = pixelFont
                horizontalAlign = HAlign.CENTER

                onUpdate += {
                    visible = !gameOver && started
                    text = "$score"
                }
            }
            textureRect {
                anchorRight = 1f
                anchorTop = 0.2f
                stretchMode = TextureRect.StretchMode.KEEP_CENTERED
                slice = atlas.getByPrefix("gameOverText").slice
                onUpdate += {
                    visible = gameOver
                }
            }

            textureRect {
                anchorRight = 1f
                anchorTop = 0.2f
                stretchMode = TextureRect.StretchMode.KEEP_CENTERED
                slice = atlas.getByPrefix("getReadyText").slice
                onUpdate += {
                    visible = !gameOver && !started
                }
            }

        }.also { it.initialize() }

        val bird = Bird(atlas.getAnimation("bird"), 12f, 10f).apply {
            y = 256 / 2f
        }

        val backgrounds = List(7) {
            val bg = atlas.getByPrefix("cityBackground").slice
            TexturedEnvironmentObject(bg, totalToWait = 2, hasCollision = false).apply {
                x = it * bg.width.toFloat() - (bg.width * 2)
            }
        }

        val groundTiles = List(35) {
            val tileIdx = Random.nextFloat().roundToInt() // using nextFloat to randomize getting 0 or 1 for the tile
            val tile = atlas.getByPrefix("terrainTile$tileIdx").slice
            TexturedEnvironmentObject(tile, totalToWait = 10, hasCollision = true).apply {
                x = it * tile.width.toFloat() - (tile.width * 10)
                y = 256f - tile.height
            }
        }

        val groundHeight = atlas.getByPrefix("terrainTile0").slice.height

        val totalPipesToSpawn = 10
        val pipeOffset = 100
        val pipes = List(totalPipesToSpawn) {
            Pipe(
                pipeHead = pipeHead,
                pipeBody = pipeBody,
                offsetX = pipeOffset * totalPipesToSpawn,
                availableHeight = gameCamera.virtualHeight,
                groundOffset = groundHeight
            ).apply {
                x = pipeOffset.toFloat() + pipeOffset * it
            }
        }

        fun reset() {
            started = false
            gameOver = false
            score = 0

            gameCamera.position.x = 0f
            bird.x = 0f
            bird.y = 256 / 2f
            bird.update(Duration.ZERO)
            bird.speedMultiplier = 1f
            bird.gravityMultiplier = 1f

            backgrounds.forEachIndexed { index, bg ->
                bg.apply {
                    x = index * texture.width.toFloat() - (texture.width * 2)
                }
            }

            groundTiles.forEachIndexed { index, tile ->
                tile.apply {
                    x = index * texture.width.toFloat() - (texture.width * 10)
                    y = 256f - texture.height
                }
            }

            pipes.forEachIndexed { index, pipe ->
                pipe.apply {
                    x = pipeOffset.toFloat() + pipeOffset * index
                    generate()
                }
            }
        }

        fun handleGameLogic(dt: Duration) {
            run pipeCollisionCheck@{
                pipes.forEach {
                    if (it.isColliding(bird.collider)) {
                        bird.speedMultiplier = 0f
                        gameOver = true
                        return@pipeCollisionCheck
                    } else if(it.intersectingScore(bird.collider)) {
                        it.collect()
                        score++
                    }
                }
            }

            run groundCollisionCheck@{
                groundTiles.forEach {
                    if (it.isColliding(bird.collider)) {
                        bird.gravityMultiplier = 0f
                        bird.speedMultiplier = 0f
                        gameOver = true
                        return@groundCollisionCheck
                    }
                }
            }

            bird.update(dt)

            if (gameOver) {
                if (input.isJustTouched(Pointer.POINTER1)) {
                    reset()
                }

            } else {
                if (input.isJustTouched(Pointer.POINTER1)) {
                    bird.flap()
                }
            }
        }

        fun handleStartMenu() {
            if (context.input.isJustTouched(Pointer.POINTER1)) {
                started = true
            }
        }

        onResize { width, height ->
            gameCamera.update(width, height, context)
            ui.resize(width, height, true)
        }

        onRender { dt ->
            gl.clearColor(Color.CLEAR)
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
            if (started) {
                handleGameLogic(dt)
            } else {
                handleStartMenu()
            }

            gameCamera.position.x = bird.x.roundToInt() + 20f
            gameCamera.viewport.apply(this)
            gameCamera.update()
            viewBounds.calculateViewBounds(gameCamera)

            backgrounds.forEach {
                it.update(viewBounds)
            }
            groundTiles.forEach {
                it.update(viewBounds)
            }
            pipes.forEach {
                it.update(viewBounds)
            }

            gl.clearColor(Color.CLEAR)
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
            batch.use(gameCamera.viewProjection) { batch ->
                backgrounds.forEach { it.render(batch) }
                groundTiles.forEach { it.render(batch) }
                pipes.forEach { it.render(batch) }
                bird.render(batch)
            }

            ui.update(dt)
            ui.render()
        }

        onPostRender {
            if (input.isKeyJustPressed(Key.P)) {
                logger.info { stats }
            }

            if (input.isKeyJustPressed(Key.R)) {
                reset()
            }
        }
        onDispose {
            atlas.dispose()
        }
    }

}

private class Bird(flapAnimation: Animation<TextureSlice>, var width: Float, var height: Float) {
    var x = 0f
    var y = 0f
    val speed = 0.06f
    val gravity = 0.018f
    var speedMultiplier = 1f
    var gravityMultiplier = 1f
    val flapHeight = -0.7f
    private val velocity = MutableVec2f(0f)
    private var sprite = flapAnimation.firstFrame
    val animationPlayer = AnimationPlayer<TextureSlice>().apply {
        onFrameChange = {
            sprite = currentAnimation?.get(it) ?: sprite
        }
        playLooped(flapAnimation)
    }

    val collider = Rect(x - width * 0.5f - height * 0.5f, y, width, height)

    fun update(dt: Duration) {
        velocity.x = speed * speedMultiplier
        velocity.y += gravity * gravityMultiplier

        x += velocity.x * dt.milliseconds
        y += velocity.y * dt.milliseconds
        animationPlayer.update(dt)

        velocity.y *= 0.91f
        if (abs(velocity.y) <= 0.0005f) {
            velocity.y = 0f
        }
        collider.set(x - width * 0.5f, y - height * 0.5f, width, height)
    }

    fun render(batch: Batch) {
        batch.draw(sprite, x, y, sprite.width * 0.5f, sprite.height * 0.5f)
    }

    fun flap() {
        velocity.y = flapHeight
    }
}

private open class EnvironmentObject(
    protected val width: Int,
    protected val totalToWait: Int,
    protected val hasCollision: Boolean,
) {
    var x: Float = 0f
    var y: Float = 0f

    open fun update(viewBounds: Rect) {
        if (x + width + (width * totalToWait) < viewBounds.x) {
            x = viewBounds.x2.roundToInt().toFloat()
            onViewBoundsReset()
        }
    }

    open fun onViewBoundsReset() = Unit

    open fun isColliding(rect: Rect) = false
}

private class TexturedEnvironmentObject(val texture: TextureSlice, totalToWait: Int, hasCollision: Boolean) :
    EnvironmentObject(texture.width, totalToWait, hasCollision) {

    fun render(batch: Batch) {
        batch.draw(texture, x, y)
    }

    override fun isColliding(rect: Rect): Boolean {
        return if (hasCollision) rect.intersects(
            x,
            y,
            x + texture.width.toFloat(),
            y + texture.height.toFloat()
        ) else false
    }
}

private class Pipe(
    private val pipeHead: TextureSlice,
    private val pipeBody: TextureSlice,
    private val offsetX: Int,
    private val availableHeight: Int,
    private val groundOffset: Int
) : EnvironmentObject(pipeBody.width, 0, true) {
    private var pipeTopHeight = 0f
    private var pipeBottomHeight = 0f

    private val topPipeBodyRect = Rect()
    private val topPipeHeadRect = Rect()

    private val bottomPipeBodyRect = Rect()
    private val bottomPipeHeadRect = Rect()

    private val scoreRect = Rect()

    private val pipeSeparationHeight = 75
    private var collected = false

    init {
        generate()
    }

    override fun update(viewBounds: Rect) {
        if (x + width + (width * 2) < viewBounds.x) {
            x += offsetX
            onViewBoundsReset()
        }

        topPipeBodyRect.set(x, y, pipeBody.width.toFloat(), pipeTopHeight)
        topPipeHeadRect.set(x, y + pipeTopHeight, pipeHead.width.toFloat(), pipeHead.height.toFloat())

        bottomPipeBodyRect.set(
            x,
            y - groundOffset + availableHeight + pipeBottomHeight,
            pipeBody.width.toFloat(),
            pipeBottomHeight
        )
        bottomPipeHeadRect.set(
            x,
            y + availableHeight - groundOffset - pipeBottomHeight + pipeHead.height.toFloat(),
            pipeHead.width.toFloat(),
            pipeHead.height.toFloat()
        )

        scoreRect.set(x + 5f, y + pipeTopHeight + pipeHead.height.toFloat(), 5f, pipeSeparationHeight.toFloat())
    }

    fun render(batch: Batch) {
        // draw top pipe
        batch.draw(pipeBody, x, y, height = pipeTopHeight)
        batch.draw(pipeHead, x, y + pipeTopHeight, flipY = true)

        // draw bottom pipe
        batch.draw(
            slice = pipeBody,
            x = x,
            y = y - groundOffset + availableHeight,
            originY = pipeBottomHeight,
            height = pipeBottomHeight
        )
        batch.draw(
            slice = pipeHead,
            x = x,
            y = y + availableHeight - groundOffset - pipeBottomHeight,
            originY = pipeHead.height.toFloat()
        )
    }

    fun intersectingScore(rect: Rect) = !collected && scoreRect.intersects(rect)

    fun collect() {
        collected = true
    }

    fun generate() {
        val minPipeHeight = 5
        val availablePipeHeight = availableHeight - groundOffset - pipeHead.height * 2 - pipeSeparationHeight
        pipeTopHeight = (minPipeHeight..availablePipeHeight).random().toFloat()
        pipeBottomHeight = availablePipeHeight - pipeTopHeight
        collected = false
    }

    override fun onViewBoundsReset() {
        generate()
    }

    override fun isColliding(rect: Rect): Boolean {
        return if (hasCollision) {
            topPipeBodyRect.intersects(rect) || topPipeHeadRect.intersects(rect)
                    || bottomPipeBodyRect.intersects(rect) || bottomPipeHeadRect.intersects(
                rect
            )
        } else {
            false
        }
    }
}

