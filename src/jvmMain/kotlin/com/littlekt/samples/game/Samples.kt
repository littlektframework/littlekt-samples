package com.littlekt.samples.game

import com.littlekt.createLittleKtApp
import com.littlekt.samples.game.SampleGame

/**
 * @author Colton Daily
 * @date 12/24/2021
 */
fun main(args: Array<String>) {
    createLittleKtApp {
        width = 960
        height = 540
        title = "LittleKt - Samples"
    }.start {
        SampleGame(it)
    }
}