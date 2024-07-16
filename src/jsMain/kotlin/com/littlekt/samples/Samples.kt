package com.littlekt.samples

import com.littlekt.createLittleKtApp
import com.littlekt.graphics.Color
import com.littlekt.samples.flappybird.FlappyBird
import com.littlekt.samples.game.SampleGame
import kotlinx.browser.window


fun main() {
    createLittleKtApp {
        this.title = window.asDynamic().sampleTitle as String
    }.start {
        when (val gameId = window.asDynamic().gameId as String) {
            "flappybird" -> FlappyBird(it)
            "sampleGameScenes" -> SampleGame(it)
            else -> error("'$gameId' is not a valid game!")
        }
    }
}
