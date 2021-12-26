package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.Game
import com.lehaine.littlekt.samples.common.GameScene
import com.lehaine.littlekt.samples.scenes.PlatformerSampleScene

/**
 * @author Colton Daily
 * @date 12/24/2021
 */
class SampleGame(context: Context) : Game<GameScene>(context, PlatformerSampleScene(context)) {


}