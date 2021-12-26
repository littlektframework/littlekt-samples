package com.lehaine.littlekt.samples.scenes

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.font.GpuFont
import com.lehaine.littlekt.graphics.font.use
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkEntity
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkLevel
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkTileMap
import com.lehaine.littlekt.samples.common.GameScene
import com.lehaine.littlekt.util.viewport.FitViewport
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 12/24/2021
 */
class PlatformerSampleScene(
    val batch: SpriteBatch,
    val gpuFontRenderer: GpuFont,
    context: Context
) : GameScene(context) {

    private val atlas: TextureAtlas by load(resourcesVfs["tiles.atlas.json"])
    private val world: LDtkTileMap by load(resourcesVfs["platformer.ldtk"])
    private val level: LDtkLevel by prepare { world.levels[0] }
    private val hero: Hero by prepare { Hero(level.entities("Player")[0], atlas) }
    private val diamonds: List<LDtkEntity> by prepare { level.entities("Diamond") }

    private val camera = OrthographicCamera(graphics.width, graphics.height).apply {
        viewport = FitViewport(480, 270)
    }

    override fun create() {
        camera.translate(level.pxWidth / 2f, level.pxHeight / 2f, 0f)
    }

    override fun render(dt: Duration) {
        camera.update()
        batch.use(camera.viewProjection) {
            level.render(it, camera)
            hero.render(it, dt)
        }
        gpuFontRenderer.use(camera.viewProjection) {
            it.drawText("Diamonds left: ${diamonds.size}", 10f, 10f, 36, color = Color.WHITE)
        }
    }

    override fun resize(width: Int, height: Int) {
        camera.viewport.update(width, height, this)
    }

    override fun dispose() {
        world.dispose()
    }
}

class Hero(data: LDtkEntity, private val atlas: TextureAtlas) {
    val sprite: TextureSlice = atlas["heroIdle0.png"].slice

    var x: Float = data.x
    var y: Float = data.y
    var pivotX = data.pivotX
    var pivotY = data.pivotY

    fun render(batch: SpriteBatch, dt: Duration) {
        batch.draw(sprite, x, y, sprite.width * pivotX, sprite.height * pivotY)
    }
}