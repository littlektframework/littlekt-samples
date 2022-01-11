package com.lehaine.littlekt.samples

import com.lehaine.littlekt.createLittleKtApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * @author Colton Daily
 * @date 12/27/2021
 */
fun main() {
    val job = Job()
    val scope = CoroutineScope(job)
    scope.launch {
        createLittleKtApp {
            title = "LittleKt - Samples"
        }.start {
            SampleGame(it)
        }
    }
}