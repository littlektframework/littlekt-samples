package com.lehaine.littlekt.samples.game.scenes

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.async.KtScope
import com.lehaine.littlekt.graph.node.ui.button
import com.lehaine.littlekt.graph.node.ui.centerContainer
import com.lehaine.littlekt.graph.node.ui.label
import com.lehaine.littlekt.graph.node.ui.vBoxContainer
import com.lehaine.littlekt.graph.sceneGraph
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.samples.game.common.GameScene
import com.lehaine.littlekt.util.viewport.FitViewport
import kotlinx.coroutines.launch
import kotlin.reflect.KClass
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 2/24/2022
 */
class SelectionScene(
    private val batch: SpriteBatch,
    private val onSelection: suspend (KClass<out GameScene>) -> Unit,
    context: Context
) : GameScene(context) {

    private val graph = sceneGraph(context, FitViewport(960, 540), batch = batch) {
        centerContainer {
            anchorRight = 1f
            anchorBottom = 1f
            vBoxContainer {
                separation = 20
                label {
                    text = "Select a Sample:"
                }

                vBoxContainer {
                    separation = 10
                    button {
                        text = "Platformer - Collect all the Diamonds!"

                        onPressed += {
                            KtScope.launch {
                                onSelection(PlatformerSampleScene::class)
                            }
                        }
                    }
                    button {
                        text = "UI - RPG"
                        onPressed += {
                            KtScope.launch {
                                onSelection(RPGUIScene::class)
                            }
                        }
                    }

                    button {
                        text = "Exit"
                        onPressed += {
                            context.close()
                        }
                    }
                }
            }
        }
    }

    override suspend fun Context.show() {
        graph.initialize()
        graph.resize(graphics.width, graphics.height)
        graph.root.enabled = true
    }

    override fun Context.render(dt: Duration) {
        gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
        graph.update(dt)
        graph.render()
    }

    override fun Context.resize(width: Int, height: Int) {
        graph.resize(width, height)
    }

    override suspend fun Context.hide() {
        updateComponents.clear()
        graph.root.enabled = false
        graph.releaseFocus()
    }
}