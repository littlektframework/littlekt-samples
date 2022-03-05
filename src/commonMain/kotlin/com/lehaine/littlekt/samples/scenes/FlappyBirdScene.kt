package com.lehaine.littlekt.samples.scenes

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.graphics.getAnimation
import com.lehaine.littlekt.input.Pointer
import com.lehaine.littlekt.samples.Assets
import com.lehaine.littlekt.samples.common.GameScene
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 3/4/2022
 */
class FlappyBirdScene(context: Context) : GameScene(context) {

    private val pipeHead = Assets.atlas.getByPrefix("pipeHead")
    private val pipeBody = Assets.atlas.getByPrefix("pipeBody")
    private val flapAnimation = Assets.atlas.getAnimation("bird")
    private val birdSlice = flapAnimation[0]
    private var started = false


    override suspend fun Context.render(dt: Duration) {
        onRender {
            if (started) {
                handleGameLogic()
            } else {
                handleStartMenu()
            }
        }
    }

    private fun handleGameLogic() {

    }

    private fun handleStartMenu() {
        if (context.input.isJustTouched(Pointer.POINTER1)) {
            started = true
        }
    }
}

private class Bird() {

    fun update(dt: Duration) {

    }

    fun render() {

    }
}

private class Pipe() {

    fun update(dt: Duration) {

    }

    fun render() {

    }
}