package com.lehaine.littlekt.samples.flappybird

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.file.vfs.readAtlas
import com.lehaine.littlekt.file.vfs.readBitmapFont
import com.lehaine.littlekt.graph.node.component.HAlign
import com.lehaine.littlekt.graph.node.node2d.ui.label
import com.lehaine.littlekt.graph.sceneGraph
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.input.Pointer
import com.lehaine.littlekt.math.Rect
import com.lehaine.littlekt.util.calculateViewBounds
import com.lehaine.littlekt.util.milliseconds
import com.lehaine.littlekt.util.viewport.ExtendViewport
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 3/4/2022
 */
class FlappyBird(context: Context) : ContextListener(context) {

    private var started = false

    override suspend fun Context.start() {
        val atlas = resourcesVfs["tiles.atlas.json"].readAtlas()
        val pixelFont = resourcesVfs["m5x7_16.fnt"].readBitmapFont(preloadedTextures = listOf(atlas["m5x7_16_0"].slice))
        val pipeHead = atlas.getByPrefix("pipeHead").slice
        val pipeBody = atlas.getByPrefix("pipeBody").slice

        val batch = SpriteBatch(this)
        val gameCamera = OrthographicCamera(graphics.width, graphics.height).apply {
            viewport = ExtendViewport(135, 256)
        }
        val viewBounds = Rect()
        val ui = sceneGraph(this, ExtendViewport(270, 480)) {
            label {
                anchorRight = 1f
                anchorTop = 0.2f
                horizontalAlign = HAlign.CENTER
                font = pixelFont
                fontColor = Color.BLACK
                text = "Press Start"
            }
        }.also { it.initialize() }

        val bird = Bird(atlas.getAnimation("bird")).apply {
            y = 256 / 2f
        }

        val backgrounds = List(7) {
            val bg = atlas.getByPrefix("cityBackground").slice
            TexturedEnvironmentObject(bg, totalToWait = 2).apply {
                x = it * bg.width.toFloat() - (bg.width * 2)
            }
        }

        val groundTiles = List(30) {
            val tileIdx = Random.nextFloat().roundToInt() // using nextFloat to randomize getting 0 or 1 for the tile
            val tile = atlas.getByPrefix("terrainTile$tileIdx").slice
            TexturedEnvironmentObject(tile, totalToWait = 10).apply {
                x = it * tile.width.toFloat() - (tile.width * 10)
                y = 256f - tile.height
            }
        }

        val groundHeight = atlas.getByPrefix("terrainTile0").slice.height

        val totalPipesToSpawn = 10
        val pipes = List(totalPipesToSpawn) {
            val offset = 100
            Pipe(
                pipeHead = pipeHead,
                pipeBody = pipeBody,
                offsetX = offset * totalPipesToSpawn,
                availableHeight = gameCamera.virtualHeight,
                groundOffset = groundHeight
            ).apply {
                x = offset.toFloat() + offset * it
            }
        }

        fun handleGameLogic(dt: Duration) {
            bird.move(dt)
            if (input.isJustTouched(Pointer.POINTER1)) {
                bird.flap()
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
            bird.updateAnim(dt)

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
        }
        onDispose {
            atlas.dispose()
        }
    }

}

private class Bird(flapAnimation: Animation<TextureSlice>) {
    var x = 0f
    var y = 0f
    val speed = 0.05f
    val gravity = 0.5f
    val flapPower = 5f
    private var sprite = flapAnimation.firstFrame
    val animationPlayer = AnimationPlayer<TextureSlice>().apply {
        onFrameChange = {
            sprite = currentAnimation?.get(it) ?: sprite
        }
        playLooped(flapAnimation)
    }

    fun move(dt: Duration) {
        x += speed * dt.milliseconds
        //   y += gravity
    }

    fun updateAnim(dt: Duration) {
        animationPlayer.update(dt)
    }

    fun render(batch: Batch) {
        batch.draw(sprite, x, y, 8f, 8f)
    }

    fun flap() {
        y -= flapPower
    }
}

private open class EnvironmentObject(protected val width: Int, protected val totalToWait: Int) {
    var x: Float = 0f
    var y: Float = 0f

    open fun update(viewBounds: Rect) {
        if (x + width + (width * totalToWait) < viewBounds.x) {
            x = viewBounds.x2.roundToInt().toFloat()
            onViewBoundsReset()
        }
    }

    open fun onViewBoundsReset() = Unit
}

private class TexturedEnvironmentObject(private val texture: TextureSlice, totalToWait: Int) :
    EnvironmentObject(texture.width, totalToWait) {

    fun render(batch: Batch) {
        batch.draw(texture, x, y)
    }
}

private class Pipe(
    private val pipeHead: TextureSlice,
    private val pipeBody: TextureSlice,
    private val offsetX: Int,
    private val availableHeight: Int,
    private val groundOffset: Int
) :
    EnvironmentObject(pipeBody.width, 0) {
    private var pipeTopHeight = 0f
    private var pipeBottomHeight = 0f

    init {
        generate()
    }

    override fun update(viewBounds: Rect) {
        if (x + width + (width * 2) < viewBounds.x) {
            x += offsetX
            onViewBoundsReset()
        }
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

    fun generate() {
        val pipeSeparationHeight = 50
        val minPipeHeight = 5
        val availablePipeHeight = availableHeight - groundOffset - pipeHead.height * 2 - pipeSeparationHeight
        pipeTopHeight = (minPipeHeight..availablePipeHeight).random().toFloat()
        pipeBottomHeight = availablePipeHeight - pipeTopHeight
    }

    override fun onViewBoundsReset() {
        generate()
    }
}

