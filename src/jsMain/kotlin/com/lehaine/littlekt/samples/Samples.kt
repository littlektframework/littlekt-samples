package com.lehaine.littlekt.samples

import com.lehaine.littlekt.createLittleKtApp
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.samples.flappybird.FlappyBird
import com.lehaine.littlekt.samples.game.SampleGame
import kotlinx.browser.window


fun main() {
    createLittleKtApp {
        this.title = window.asDynamic().sampleTitle as String
        backgroundColor = Color.DARK_GRAY
    }.start {
        when (val gameId = window.asDynamic().gameId as String) {
            "flappybird" -> FlappyBird(it)
            "sampleGameScenes" -> SampleGame(it)
            else -> error("'$gameId' is not a valid game!")
        }
    }
}
