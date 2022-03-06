package com.lehaine.littlekt.samples

import com.lehaine.littlekt.createLittleKtApp
import com.lehaine.littlekt.samples.game.SampleGame

/**
 * @author Colton Daily
 * @date 12/24/2021
 */
fun main(args: Array<String>) {
    createLittleKtApp {
        width = 960
        height = 540
        vSync = true
        title = "LittleKt - Samples"
    }.start {
        SampleGame(it)
    }
}