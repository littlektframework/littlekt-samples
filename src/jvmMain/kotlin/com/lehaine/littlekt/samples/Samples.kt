package com.lehaine.littlekt.samples

import com.lehaine.littlekt.createLittleKtApp
import kotlinx.coroutines.runBlocking

/**
 * @author Colton Daily
 * @date 12/24/2021
 */
fun main(args: Array<String>) {
    runBlocking {
        createLittleKtApp {
            width = 960
            height = 540
            vSync = true
            title = "LittleKt - Samples"
        }.start {
            SampleGame(it)
        }
    }
}