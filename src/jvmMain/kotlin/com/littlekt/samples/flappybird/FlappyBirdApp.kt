package com.littlekt.samples.flappybird

import com.littlekt.createLittleKtApp

/**
 * @author Colton Daily
 * @date 3/5/2022
 */
fun main(args: Array<String>) {
    createLittleKtApp {
        width = 540
        height = 1024
        title = "LittleKt - Flappy Bird CLone"
    }.start {
        FlappyBird(it)
    }
}