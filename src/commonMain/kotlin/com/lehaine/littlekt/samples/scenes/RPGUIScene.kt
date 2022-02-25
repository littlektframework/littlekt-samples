package com.lehaine.littlekt.samples.scenes

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.graph.node.node2d.ui.*
import com.lehaine.littlekt.graph.sceneGraph
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.SpriteBatch
import com.lehaine.littlekt.graphics.TextureAtlas
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.samples.Assets
import com.lehaine.littlekt.samples.common.GameScene
import com.lehaine.littlekt.util.viewport.FitViewport
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 2/24/2022
 */
class RPGUIScene(
    val batch: SpriteBatch,
    context: Context
) : GameScene(context) {
    private val atlas: TextureAtlas get() = Assets.atlas

    private val graph =
        sceneGraph(context, FitViewport(960, 540), batch = batch) {
            paddedContainer {
                name = "GUI"
                padding(20)
                anchor(Control.AnchorLayout.TOP_WIDE)

                hBoxContainer {
                    vBoxContainer {
                        name = "Bars"

                        hBoxContainer {
                            name = "Health Bar"

                            textureProgress {
                                background = atlas.getByPrefix("healthBarBg").slice
                                progressBar = atlas.getByPrefix("healthBarProgress").slice
                                foreground = atlas.getByPrefix("healthBarFg").slice

                                value = 87f
                            }
                        }


                        hBoxContainer {
                            name = "Mana Bar"

                            textureProgress {
                                background = atlas.getByPrefix("manaBarBg").slice
                                progressBar = atlas.getByPrefix("manaBarProgress").slice
                                foreground = atlas.getByPrefix("manaBarFg").slice

                                value = 75f
                            }
                        }
                    }

                    hBoxContainer {
                        name = "Counters"
                    }
                }
            }
        }.also { it.initialize() }


    override suspend fun Context.show() {
        graph.root.enabled = true
    }

    override suspend fun Context.render(dt: Duration) {
        gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
        gl.clearColor(Color.DARK_GRAY)
        graph.update(dt)
        graph.render()
    }

    override suspend fun Context.resize(width: Int, height: Int) {
        graph.resize(width, height)
    }

    override suspend fun Context.hide() {
        updateComponents.clear()
        graph.root.enabled = false
        graph.releaseFocus()
    }
}