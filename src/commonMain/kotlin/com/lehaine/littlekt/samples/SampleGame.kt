package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.Game
import com.lehaine.littlekt.file.vfs.readBitmapFont
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.log.Logger
import com.lehaine.littlekt.samples.common.GameScene
import com.lehaine.littlekt.samples.scenes.PlatformerSampleScene

/**
 * @author Colton Daily
 * @date 12/24/2021
 */
class SampleGame(context: Context) : Game<GameScene>(context) {

    private val batch = SpriteBatch(context)

    init {
        Logger.setLevels(Logger.Level.DEBUG)
    }

    override suspend fun Context.start() {
        super.setSceneCallbacks(this)
        val pixelFont = resourcesVfs["m5x7_16.fnt"].readBitmapFont()
        addScene(PlatformerSampleScene(batch, pixelFont, context))
        setScene<PlatformerSampleScene>()
        onRender {
            if (input.isKeyJustPressed(Key.ESCAPE)) {
                close()
            }
        }
        onDispose {
            batch.dispose()
        }
    }
}