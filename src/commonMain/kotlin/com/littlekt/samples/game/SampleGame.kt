package com.littlekt.samples.game

import com.littlekt.AssetProvider
import com.littlekt.Context
import com.littlekt.Releasable
import com.littlekt.async.KtScope
import com.littlekt.audio.AudioClip
import com.littlekt.file.ldtk.LDtkMapLoader
import com.littlekt.graphics.g2d.TextureAtlas
import com.littlekt.graphics.g2d.font.BitmapFont
import com.littlekt.graphics.g2d.tilemap.ldtk.LDtkWorld
import com.littlekt.input.Key
import com.littlekt.log.Logger
import com.littlekt.samples.game.common.Game
import com.littlekt.samples.game.common.GameScene
import com.littlekt.samples.game.scenes.PlatformerSampleScene
import com.littlekt.samples.game.scenes.RPGUIScene
import com.littlekt.samples.game.scenes.SelectionScene
import kotlinx.coroutines.launch
import kotlin.concurrent.Volatile
import kotlin.reflect.KClass

/**
 * @author Colton Daily
 * @date 12/24/2021
 */
class SampleGame(context: Context) : Game<GameScene>(context) {

    init {
        Logger.setLevels(Logger.Level.DEBUG)
    }

    private suspend fun <T : GameScene> onSelection(scene: KClass<out T>) = setScene(scene)

    override suspend fun Context.start() {
        setSceneCallbacks(this)
        Assets.createInstance(this) {
            KtScope.launch {
                addScene(SelectionScene::class, SelectionScene(::onSelection, context))
                addScene(PlatformerSampleScene::class, PlatformerSampleScene(context))
                addScene(RPGUIScene::class, RPGUIScene(context))
                setScene<SelectionScene>()
            }
        }

        onUpdate {
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

        onPostUpdate {
            if (input.isKeyJustPressed(Key.P)) {
                logger.info { stats }
            }
        }
    }
}

class Assets private constructor(context: Context) : Releasable {
    private val assets = AssetProvider(context)
    private val atlas: TextureAtlas by assets.load(context.resourcesVfs["tiles.atlas.json"])
    private val pixelFont: BitmapFont by assets.load(context.resourcesVfs["m5x7_16.fnt"])

    private val sfxFootstep: AudioClip by assets.load(context.resourcesVfs["sfx/footstep0.wav"])
    private val sfxLand: AudioClip by assets.load(context.resourcesVfs["sfx/land0.wav"])
    private val sfxPickup: AudioClip by assets.load(context.resourcesVfs["sfx/pickup0.wav"])
    private val platformerMapLoader: LDtkMapLoader by assets.load(context.resourcesVfs["platformer.ldtk"])
    private val platformerWorld: LDtkWorld by assets.prepare { platformerMapLoader.loadMap(false) }

    override fun release() {
        atlas.release()
        pixelFont.release()
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
            context.onUpdate { INSTANCE.assets.update() }
            return newInstance
        }

        fun release() {
            instance?.release()
        }
    }
}