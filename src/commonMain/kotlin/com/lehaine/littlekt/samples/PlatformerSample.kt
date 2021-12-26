package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextScene
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.OrthographicCamera
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.graphics.font.GpuFont
import com.lehaine.littlekt.graphics.font.TtfFont
import com.lehaine.littlekt.graphics.font.use
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkEntity
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkLevel
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkTileMap
import com.lehaine.littlekt.graphics.use
import com.lehaine.littlekt.log.Logger
import com.lehaine.littlekt.util.viewport.FitViewport
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 12/24/2021
 */
class PlatformerSample(context: Context) : ContextScene(context) {

    private val world: LDtkTileMap by load(resourcesVfs["platformer.ldtk"])
    private val level: LDtkLevel by prepare { world.levels[0] }
    private val diamonds: List<LDtkEntity> by prepare { level.entities("Diamond") }
    private val batch = SpriteBatch(this)
    private val pixelFont: TtfFont by load(resourcesVfs["m5x7.ttf"])
    private val gpuFontRenderer by prepare { GpuFont(this, pixelFont) }
    private val camera = OrthographicCamera(graphics.width, graphics.height).apply {
        viewport = FitViewport(480, 270)
    }

    init {
        Logger.defaultLevel = Logger.Level.DEBUG
        logger.level = Logger.Level.DEBUG
    }

    override fun prepare() {
        camera.translate(level.pxWidth / 2f, level.pxHeight / 2f, 0f)
    }

    override fun update(dt: Duration) {
        camera.update()
        batch.use(camera.viewProjection) {
            level.render(it, camera)
        }
        gpuFontRenderer.use(camera.viewProjection) {
            it.drawText("Diamonds left: ${diamonds.size}", 10f, 10f, 36, color = Color.WHITE)
        }
    }

    override fun resize(width: Int, height: Int) {
        camera.viewport.update(width, height, this)
    }
}