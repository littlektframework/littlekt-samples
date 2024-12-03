package com.littlekt.samples.flappybird

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.async.KtScope
import com.littlekt.async.newSingleThreadAsyncContext
import com.littlekt.file.vfs.readAtlas
import com.littlekt.file.vfs.readAudioClip
import com.littlekt.file.vfs.readBitmapFont
import com.littlekt.graph.node.resource.InputEvent
import com.littlekt.graph.node.resource.NinePatchDrawable
import com.littlekt.graph.node.ui.*
import com.littlekt.graph.sceneGraph
import com.littlekt.graphics.Color
import com.littlekt.graphics.HAlign
import com.littlekt.graphics.g2d.*
import com.littlekt.graphics.g2d.shape.ShapeRenderer
import com.littlekt.graphics.webgpu.*
import com.littlekt.input.Key
import com.littlekt.input.Pointer
import com.littlekt.math.MutableVec2f
import com.littlekt.math.Rect
import com.littlekt.util.calculateViewBounds
import com.littlekt.util.milliseconds
import com.littlekt.util.viewport.ExtendViewport
import kotlinx.coroutines.launch
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
    private var best = 0
    private var paused = false

    override suspend fun Context.start() {
        val atlas = resourcesVfs["tiles.atlas.json"].readAtlas()
        val pixelFont =
            resourcesVfs["m5x7_16_outline.fnt"].readBitmapFont(preloadedTextures = listOf(atlas["m5x7_16_outline_0"].slice))
        val pipeHead = atlas.getByPrefix("pipeHead").slice
        val pipeBody = atlas.getByPrefix("pipeBody").slice
        val pauseSlice = atlas.getByPrefix("pauseButton").slice
        val resumeSlice = atlas.getByPrefix("resumeButton").slice
        val startButton = atlas.getByPrefix("startButton").slice
        val panel9Slice = atlas.getByPrefix("panel_9").slice

        val audioCtx = newSingleThreadAsyncContext()
        val flapSfx = resourcesVfs["sfx/flap.wav"].readAudioClip()
        val scoreSfx = resourcesVfs["sfx/coinPickup0.wav"].readAudioClip()

        val device = graphics.device
        val preferredFormat = graphics.preferredFormat
        val batch = SpriteBatch(device, graphics, preferredFormat)
        val shapeRenderer = ShapeRenderer(batch)
        val gameViewport = ExtendViewport(135, 256)
        val gameCamera = gameViewport.camera
        val viewBounds = Rect()

        val bird = Bird(atlas.getAnimation("bird"), 12f, 10f).apply {
            y = 256 / 2f
            update(Duration.ZERO)
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
                y = 0f
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
                availableHeight = gameViewport.virtualHeight.toInt(),
                groundOffset = groundHeight
            ).apply {
                x = pipeOffset.toFloat() + pipeOffset * it
            }
        }
        var debug = false

        fun reset() {
            started = false
            gameOver = false
            score = 0

            gameCamera.position.x = 0f
            bird.x = 0f
            bird.y = 256 / 2f
            bird.update(Duration.ZERO)
            bird.reset()

            backgrounds.forEachIndexed { index, bg ->
                bg.apply {
                    x = index * texture.width.toFloat() - (texture.width * 2)
                }
            }

            groundTiles.forEachIndexed { index, tile ->
                tile.apply {
                    x = index * texture.width.toFloat() - (texture.width * 10)
                    y = 0f
                }
            }

            pipes.forEachIndexed { index, pipe ->
                pipe.apply {
                    x = pipeOffset.toFloat() + pipeOffset * index
                    generate()
                }
            }
        }

        val ui = sceneGraph(this, ExtendViewport(135, 256)) {
            textureRect {
                anchor(Control.AnchorLayout.TOP_LEFT)
                marginLeft = 10f
                marginTop = 10f
                slice = pauseSlice

                onUpdate += {
                    visible = !gameOver && started && !paused
                }

                onUiInput += uiInput@{
                    if (!gameOver && started && !paused) {
                        if (it.type == InputEvent.Type.TOUCH_DOWN) {
                            paused = true
                            it.handle()
                        }
                    }
                }
            }

            panel {
                name = "Score Container"

                panel = NinePatchDrawable(NinePatch(panel9Slice, 2, 2, 2, 4))

                anchorLeft = 0.1f
                anchorRight = 0.9f
                anchorTop = 0.8f
                anchorBottom = 0.3f

                onUpdate += {
                    visible = gameOver
                }

                vBoxContainer {
                    separation = 10
                    marginBottom = 5f
                    anchorRight = 1f
                    anchorTop = 1f

                    label {
                        font = pixelFont
                        horizontalAlign = HAlign.CENTER

                        onUpdate += {
                            text = "Score: $score"
                        }
                    }

                    label {
                        font = pixelFont
                        horizontalAlign = HAlign.CENTER

                        onUpdate += {
                            text = "Best Score: $best"
                        }
                    }
                }

                textureRect {
                    anchorTop = 1f
                    anchorBottom = 1f
                    anchorRight = 1f

                    marginBottom = -50f
                    slice = startButton
                    stretchMode = TextureRect.StretchMode.KEEP_CENTERED

                    onUiInput += {
                        if (gameOver) {
                            if (it.type == InputEvent.Type.TOUCH_DOWN) {
                                reset()
                            }
                        }
                    }
                }
            }

            centerContainer {
                anchorRight = 1f
                anchorTop = 1f
                onUpdate += {
                    visible = paused
                }

                vBoxContainer {
                    separation = 10

                    label {
                        text = "Tap to Resume"
                        font = pixelFont
                        horizontalAlign = HAlign.CENTER
                    }

                    textureRect {
                        slice = resumeSlice
                        stretchMode = TextureRect.StretchMode.KEEP_CENTERED
                        onUiInput += uiInput@{
                            if (paused) {
                                if (it.type == InputEvent.Type.TOUCH_DOWN) {
                                    paused = false
                                    it.handle()
                                }
                            }
                        }
                    }
                }
            }

            textureRect {
                anchor(Control.AnchorLayout.TOP_LEFT)
                marginLeft = 10f
                marginTop = 10f
                slice = pauseSlice

                onUpdate += {
                    visible = !gameOver && started && !paused
                }

                onUiInput += {
                    if (it.type == InputEvent.Type.TOUCH_DOWN) {
                        paused = !paused
                        it.handle()
                    }
                }
            }
            label {
                anchorRight = 1f
                anchorBottom = 0.9f
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
                anchorBottom = 0.8f
                stretchMode = TextureRect.StretchMode.KEEP_CENTERED
                slice = atlas.getByPrefix("gameOverText").slice
                onUpdate += {
                    visible = gameOver
                }
            }

            textureRect {
                anchorRight = 1f
                anchorBottom = 0.8f
                stretchMode = TextureRect.StretchMode.KEEP_CENTERED
                slice = atlas.getByPrefix("getReadyText").slice
                onUpdate += {
                    visible = !gameOver && !started
                }
            }

        }.also { it.initialize() }

        fun saveScore() {
            best = kvStorage.loadString("best")?.toInt() ?: 0
            if (score > best) {
                kvStorage.store("best", score.toString())
                best = score
            }
        }

        fun handleGameLogic(dt: Duration) {
            run pipeCollisionCheck@{
                pipes.forEach {
                    if (it.isColliding(bird.collider)) {
                        bird.speedMultiplier = 0f
                        gameOver = true
                        saveScore()
                        return@pipeCollisionCheck
                    } else if (it.intersectingScore(bird.collider)) {
                        KtScope.launch(audioCtx) {
                            scoreSfx.play(0.5f)
                        }
                        it.collect()
                        score++
                    }
                }
            }

            run groundCollisionCheck@{
                groundTiles.forEach {
                    if (it.isColliding(bird.collider)) {
                        bird.die()
                        gameOver = true
                        saveScore()
                        return@groundCollisionCheck
                    }
                }
            }

            bird.update(dt)
            if (bird.y < 0) {
                bird.y = 0f
            }

            if (!gameOver) {
                if (input.isJustTouched(Pointer.POINTER1)) {
                    bird.flap()
                    KtScope.launch(audioCtx) {
                        flapSfx.play()
                    }
                }
            }
        }

        fun handleStartMenu() {
            if (input.isJustTouched(Pointer.POINTER1)) {
                started = true
            }
        }

        val surfaceCapabilities = graphics.surfaceCapabilities
        onResize { width, height ->
            gameViewport.update(width, height, true)
            ui.resize(width, height, true)
            graphics.configureSurface(
                TextureUsage.RENDER_ATTACHMENT, preferredFormat, PresentMode.FIFO, surfaceCapabilities.alphaModes[0]
            )
        }

        onUpdate { dt ->
            if (started && !paused) {
                handleGameLogic(dt)
            } else {
                handleStartMenu()
            }

            if (context.input.isKeyPressed(Key.SHIFT_LEFT) && context.input.isKeyJustPressed(Key.D)) {
                debug = !debug
            }

            gameCamera.position.x = bird.x.roundToInt() + 20f
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

            val surfaceTexture = context.graphics.surface.getCurrentTexture()
            when (val status = surfaceTexture.status) {
                TextureStatus.SUCCESS -> {
                    // all good, could check for `surfaceTexture.suboptimal` here.
                }

                TextureStatus.TIMEOUT, TextureStatus.OUTDATED, TextureStatus.LOST -> {
                    surfaceTexture.texture?.release()
                    logger.info { "getCurrentTexture status=$status" }
                    return@onUpdate
                }

                else -> {
                    // fatal
                    logger.fatal { "getCurrentTexture status=$status" }
                    close()
                    return@onUpdate
                }
            }
            val swapChainTexture = checkNotNull(surfaceTexture.texture)
            val frame = swapChainTexture.createView()

            val commandEncoder = device.createCommandEncoder()
            val gameRenderPassEncoder = commandEncoder.beginRenderPass(
                desc = RenderPassDescriptor(
                    listOf(
                        RenderPassColorAttachmentDescriptor(
                            view = frame,
                            loadOp = LoadOp.CLEAR,
                            storeOp = StoreOp.STORE,
                            clearColor = if (preferredFormat.srgb) Color.DARK_GRAY.toLinear()
                            else Color.DARK_GRAY
                        )
                    ), label = "Game render pass"
                )
            )

            batch.use(gameRenderPassEncoder, gameCamera.viewProjection) {
                backgrounds.forEach { it.render(batch) }
                groundTiles.forEach { it.render(batch) }
                bird.render(batch)
                pipes.forEach { it.render(batch) }
                if (debug) {
                    bird.debugRender(shapeRenderer)
                    groundTiles.forEach { it.debugRender(shapeRenderer) }
                    pipes.forEach { it.debugRender(shapeRenderer) }
                }
            }
            gameRenderPassEncoder.end()

            val renderPassDescriptor = RenderPassDescriptor(
                listOf(
                    RenderPassColorAttachmentDescriptor(
                        view = frame,
                        loadOp = LoadOp.LOAD,
                        storeOp = StoreOp.STORE
                    )
                ), label = "Scene Graph render pass"
            )


            ui.update(dt)
            ui.render(commandEncoder, renderPassDescriptor)

            val commandBuffer = commandEncoder.finish()

            device.queue.submit(commandBuffer)
            graphics.surface.present()

            commandBuffer.release()
            gameRenderPassEncoder.release()
            commandEncoder.release()
            frame.release()
            swapChainTexture.release()
        }

        onPostUpdate {
            if (input.isKeyJustPressed(Key.P)) {
                logger.info { stats }
            }

            if (input.isKeyJustPressed(Key.R)) {
                reset()
            }
        }
        onRelease {
            atlas.release()
        }
    }

}

private class Bird(private val flapAnimation: Animation<TextureSlice>, var width: Float, var height: Float) {
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
        y -= velocity.y * dt.milliseconds
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

    fun debugRender(shapeRenderer: ShapeRenderer) {
        shapeRenderer.rectangle(collider, color = Color.GREEN)
    }

    fun flap() {
        velocity.y = flapHeight
    }

    fun die() {
        velocity.y = 0f
        velocity.x = 0f
        gravityMultiplier = 0f
        speedMultiplier = 0f
        animationPlayer.stop()
    }

    fun reset() {
        gravityMultiplier = 1f
        speedMultiplier = 1f
        animationPlayer.playLooped(flapAnimation)
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

    fun debugRender(shapeRenderer: ShapeRenderer) {
        shapeRenderer.rectangle(x, y, texture.width.toFloat(), texture.height.toFloat(), color = Color.CYAN)
    }

    override fun isColliding(rect: Rect): Boolean {
        return if (hasCollision) rect.intersects(
            x, y, x + texture.width.toFloat(), y + texture.height.toFloat()
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

        topPipeBodyRect.set(x, y + availableHeight - pipeTopHeight, pipeBody.width.toFloat(), pipeTopHeight)
        topPipeHeadRect.set(
            x,
            y + availableHeight - pipeTopHeight - pipeHead.height.toFloat(),
            pipeHead.width.toFloat(), pipeHead.height.toFloat()
        )

        bottomPipeBodyRect.set(
            x, y + groundOffset, pipeBody.width.toFloat(), pipeBottomHeight
        )
        bottomPipeHeadRect.set(
            x,
            y + groundOffset + pipeBottomHeight,
            pipeHead.width.toFloat(),
            pipeHead.height.toFloat()
        )

        scoreRect.set(
            x + 5f,
            y + groundOffset + pipeBottomHeight + pipeHead.height.toFloat(),
            5f,
            pipeSeparationHeight.toFloat()
        )
    }

    fun render(batch: Batch) {
        // draw top pipe
        batch.draw(pipeBody, x, y + availableHeight, originY = pipeTopHeight, height = pipeTopHeight)
        batch.draw(
            pipeHead,
            x,
            y + availableHeight - pipeTopHeight,
            originY = pipeHead.actualHeight.toFloat(),
            flipY = true
        )

//        // draw bottom pipe
        batch.draw(
            slice = pipeBody,
            x = x,
            y = y + groundOffset,
            height = pipeBottomHeight
        )
        batch.draw(
            slice = pipeHead,
            x = x,
            y = y + groundOffset + pipeBottomHeight,
        )
    }

    fun debugRender(shapeRenderer: ShapeRenderer) {
        shapeRenderer.color = Color.DARK_YELLOW
        shapeRenderer.rectangle(topPipeBodyRect)
        shapeRenderer.rectangle(topPipeHeadRect)

        shapeRenderer.color = Color.DARK_RED
        shapeRenderer.rectangle(bottomPipeBodyRect)
        shapeRenderer.rectangle(bottomPipeHeadRect)

        shapeRenderer.color = Color.LIGHT_GREEN
        shapeRenderer.rectangle(scoreRect)
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
            topPipeBodyRect.intersects(rect) || topPipeHeadRect.intersects(rect) || bottomPipeBodyRect.intersects(rect) || bottomPipeHeadRect.intersects(
                rect
            )
        } else {
            false
        }
    }
}

