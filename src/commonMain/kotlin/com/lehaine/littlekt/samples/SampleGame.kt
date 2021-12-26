package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.Game
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.graphics.font.GpuFont
import com.lehaine.littlekt.graphics.font.TtfFont
import com.lehaine.littlekt.log.Logger
import com.lehaine.littlekt.samples.common.GameScene
import com.lehaine.littlekt.samples.scenes.PlatformerSampleScene

/**
 * @author Colton Daily
 * @date 12/24/2021
 */
class SampleGame(context: Context) : Game<GameScene>(context) {

    private val batch = SpriteBatch(this)
    private val pixelFont: TtfFont by load(resourcesVfs["m5x7.ttf"])
    private val gpuFontRenderer by prepare { GpuFont(this, pixelFont) }

    init {
        Logger.defaultLevel = Logger.Level.DEBUG
        logger.level = Logger.Level.DEBUG
    }

    override fun create() {
        addScene(PlatformerSampleScene(batch, gpuFontRenderer, context))
        setScene<PlatformerSampleScene>()
    }
}