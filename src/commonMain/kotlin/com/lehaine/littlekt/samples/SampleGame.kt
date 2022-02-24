package com.lehaine.littlekt.samples

import com.lehaine.littlekt.AssetProvider
import com.lehaine.littlekt.Context
import com.lehaine.littlekt.Game
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.log.Logger
import com.lehaine.littlekt.samples.common.GameScene
import com.lehaine.littlekt.samples.scenes.ComplexUIScene
import com.lehaine.littlekt.samples.scenes.PlatformerSampleScene
import com.lehaine.littlekt.samples.scenes.SelectionScene
import kotlin.reflect.KClass

/**
 * @author Colton Daily
 * @date 12/24/2021
 */
class SampleGame(context: Context) : Game<GameScene>(context) {

    private val batch = SpriteBatch(context)
    private val assets = AssetProvider(context)

    init {
        Logger.setLevels(Logger.Level.DEBUG)
    }

    private suspend fun <T : GameScene> onSelection(scene: KClass<out T>) = setScene(scene)

    override suspend fun Context.start() {
        super.setSceneCallbacks(this)
        addScene(SelectionScene(batch, assets, ::onSelection, context))
        addScene(PlatformerSampleScene(batch, assets, context))
        addScene(ComplexUIScene(batch, assets, context))
        setScene<SelectionScene>()

        onRender {
            assets.update()
            if (input.isKeyPressed(Key.SHIFT_LEFT) && input.isKeyJustPressed(Key.BACKSPACE)) {
                if (currentScene !is SelectionScene) {
                    setScene<SelectionScene>()
                }
            }
            if (input.isKeyJustPressed(Key.ESCAPE)) {
                close()
            }
        }
        onDispose {
            batch.dispose()
        }
    }
}