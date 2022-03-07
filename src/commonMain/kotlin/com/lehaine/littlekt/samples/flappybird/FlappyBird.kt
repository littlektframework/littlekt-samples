package com.lehaine.littlekt.samples.flappybird

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.file.vfs.readAtlas
import com.lehaine.littlekt.file.vfs.readBitmapFont
import com.lehaine.littlekt.file.vfs.readTexture
import com.lehaine.littlekt.graph.node.component.HAlign
import com.lehaine.littlekt.graph.node.node2d.ui.label
import com.lehaine.littlekt.graph.sceneGraph
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.input.Pointer
import com.lehaine.littlekt.math.Rect
import com.lehaine.littlekt.math.floorToInt
import com.lehaine.littlekt.util.MutableTextureAtlas
import com.lehaine.littlekt.util.Scaler
import com.lehaine.littlekt.util.calculateViewBounds
import com.lehaine.littlekt.util.milliseconds
import com.lehaine.littlekt.util.viewport.ExtendViewport
import com.lehaine.littlekt.util.viewport.ScalingViewport
import kotlin.math.max
import kotlin.math.min
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
        val atlas = MutableTextureAtlas(this).run {
            add(resourcesVfs["tiles.atlas.json"].readAtlas())
            val pixelFontBitmap = resourcesVfs["m5x7_16_0.png"].readTexture()
            add(pixelFontBitmap.slice(), "pixelFont")
            val immutable = toImmutable()
            pixelFontBitmap.dispose()
            immutable
        }
        val pixelFont = resourcesVfs["m5x7_16.fnt"].readBitmapFont(preloadedTextures = listOf(atlas["pixelFont"].slice))
        val pipeHead = atlas.getByPrefix("pipeHead")
        val pipeBody = atlas.getByPrefix("pipeBody")

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
            EnvironmentObject(bg, totalToWait = 2).apply {
                x = it * bg.width.toFloat() - (bg.width * 2)
            }
        }

        val groundTiles = List(30) {
            val tileIdx = Random.nextFloat().roundToInt() // using nextFloat to randomize getting 0 or 1 for the tile
            val tile = atlas.getByPrefix("terrainTile$tileIdx").slice
            EnvironmentObject(tile, totalToWait = 10).apply {
                x = it * tile.width.toFloat() - (tile.width * 10)
                y = 256f - tile.height
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

            gl.clearColor(Color.CLEAR)
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
            batch.use(gameCamera.viewProjection) { batch ->
                backgrounds.forEach { it.render(batch) }
                groundTiles.forEach { it.render(batch) }
                bird.render(batch)
            }

            ui.update(dt)
            ui.render()
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

private class Pipe() {

    fun update(dt: Duration) {

    }

    fun render() {

    }
}

private class EnvironmentObject(private val texture: TextureSlice, private val totalToWait: Int) {
    var x: Float = 0f
    var y: Float = 0f

    fun update(viewBounds: Rect) {
        if (x + texture.width + (texture.width * totalToWait) < viewBounds.x) {
            x = viewBounds.x2.roundToInt().toFloat()
        }
    }

    fun render(batch: Batch) {
        batch.draw(texture, x, y)
    }
}