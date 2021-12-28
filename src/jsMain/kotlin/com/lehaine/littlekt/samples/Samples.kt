package com.lehaine.littlekt.samples

import com.lehaine.littlekt.createLittleKtApp

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