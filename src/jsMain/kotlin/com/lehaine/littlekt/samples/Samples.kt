package com.lehaine.littlekt.samples

import com.lehaine.littlekt.createLittleKtApp
import com.lehaine.littlekt.samples.game.SampleGame

/**
 * @author Colton Daily
 * @date 12/27/2021
 */
fun main() {
    createLittleKtApp {
        title = "LittleKt - Samples"
    }.start {
        SampleGame(it)
    }
}