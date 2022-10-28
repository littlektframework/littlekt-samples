package com.lehaine.littlekt.samples.game

import com.lehaine.littlekt.AssetProvider
import com.lehaine.littlekt.Context
import com.lehaine.littlekt.Disposable
import com.lehaine.littlekt.Game
import com.lehaine.littlekt.async.KtScope
import com.lehaine.littlekt.audio.AudioClip
import com.lehaine.littlekt.file.ldtk.LDtkMapLoader
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.graphics.TextureAtlas
import com.lehaine.littlekt.graphics.font.BitmapFont
import com.lehaine.littlekt.graphics.tilemap.ldtk.LDtkWorld
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.log.Logger
import com.lehaine.littlekt.samples.game.common.GameScene
import com.lehaine.littlekt.samples.game.scenes.PlatformerSampleScene
import com.lehaine.littlekt.samples.game.scenes.RPGUIScene
import com.lehaine.littlekt.samples.game.scenes.SelectionScene
import kotlinx.coroutines.launch
import kotlin.jvm.Volatile
import kotlin.reflect.KClass

/**
 * @author Colton Daily
 * @date 12/24/2021
 */
class SampleGame(context: Context) : Game<GameScene>(context) {

    private val batch = SpriteBatch(context)

    init {
        Logger.setLevels(Logger.Level.DEBUG)
    }

    private suspend fun <T : GameScene> onSelection(scene: KClass<out T>) = setScene(scene)

    override suspend fun Context.start() {
        setSceneCallbacks(this)
        Assets.createInstance(this) {
            KtScope.launch {
                addScene(SelectionScene(batch, ::onSelection, context))
                addScene(PlatformerSampleScene(batch, context))
                addScene(RPGUIScene(batch, context))
                setScene<SelectionScene>()
            }
        }

        onRender {
            if (input.isKeyPressed(Key.SHIFT_LEFT) && input.isKeyJustPressed(Key.BACKSPACE)) {
                if (containsScene<SelectionScene>() && currentScene !is SelectionScene) {
                    KtScope.launch {
                        setScene<SelectionScene>()
                    }
                }
            }

            if (input.isKeyJustPressed(Key.ESCAPE)) {
                close()
            }
        }

        onPostRender {
            if (input.isKeyJustPressed(Key.P)) {
                logger.info { stats }
            }
        }

        onDispose {
            batch.dispose()
        }
    }
}

class Assets private constructor(context: Context) : Disposable {
    private val assets = AssetProvider(context)
    private val atlas: TextureAtlas by assets.load(context.resourcesVfs["tiles.atlas.json"])
    private val pixelFont: BitmapFont by assets.load(context.resourcesVfs["m5x7_16.fnt"])

    private val sfxFootstep: AudioClip by assets.load(context.resourcesVfs["sfx/footstep0.wav"])
    private val sfxLand: AudioClip by assets.load(context.resourcesVfs["sfx/land0.wav"])
    private val sfxPickup: AudioClip by assets.load(context.resourcesVfs["sfx/pickup0.wav"])
    private val platformerMapLoader: LDtkMapLoader by assets.load(context.resourcesVfs["platformer.ldtk"])
    private val platformerWorld: LDtkWorld by assets.prepare { platformerMapLoader.loadMap(false) }

    override fun dispose() {
        atlas.dispose()
        pixelFont.dispose()
    }

    companion object {
        @Volatile
        private var instance: Assets? = null
        private val INSTANCE: Assets get() = instance ?: error("Instance has not been created!")

        val atlas: TextureAtlas get() = INSTANCE.atlas
        val pixelFont: BitmapFont get() = INSTANCE.pixelFont
        val sfxFootstep: AudioClip get() = INSTANCE.sfxFootstep
        val sfxLand: AudioClip get() = INSTANCE.sfxLand
        val sfxPickup: AudioClip get() = INSTANCE.sfxPickup
        val platformerWorld: LDtkWorld get() = INSTANCE.platformerWorld

        fun createInstance(context: Context, onLoad: () -> Unit): Assets {
            check(instance == null) { "Instance already created!" }
            val newInstance = Assets(context)
            instance = newInstance
            INSTANCE.assets.onFullyLoaded = onLoad
            context.onRender { INSTANCE.assets.update() }
            return newInstance
        }

        fun dispose() {
            instance?.dispose()
        }
    }
}