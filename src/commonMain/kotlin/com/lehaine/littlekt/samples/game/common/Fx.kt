package com.lehaine.littlekt.samples.game.common

import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.math.random
import com.lehaine.littlekt.util.seconds
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 12/29/2021
 */
class Fx(private val atlas: TextureAtlas) {
    private val particleSimulator = ParticleSimulator(2048)

    fun update(dt: Duration, tmod: Float = -1f) {
        particleSimulator.update(dt, tmod)
    }

    fun render(batch: SpriteBatch) {
        particleSimulator.draw(batch)
    }

    fun runDust(x: Float, y: Float, dir: Int) {
        create(5) {
            val p = alloc(atlas.getByPrefix("fxSmallCircle").slice, x, y)
            p.scale((0.15f..0.25f).random())
            p.color.set(DUST_COLOR).also { p.colorBits = DUST_COLOR_BITS }
            p.xDelta = (0.25f..0.75f).random() * dir
            p.yDelta = -(0.05f..0.15f).random()
            p.life = (0.05f..0.15f).random().seconds
            p.scaleDelta = (0.005f..0.015f).random()
        }
    }

    private fun alloc(slice: TextureSlice, x: Float, y: Float) = particleSimulator.alloc(slice, x, y)

    private fun create(num: Int, createParticle: (index: Int) -> Unit) {
        for (i in 0 until num) {
            createParticle(i)
        }
    }

    companion object {
        private val DUST_COLOR = Color.fromHex("#efddc0")
        private val DUST_COLOR_BITS = DUST_COLOR.toFloatBits()
    }
}