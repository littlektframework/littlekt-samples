package com.lehaine.littlekt.samples.game.scenes

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.graph.node.resource.HAlign
import com.lehaine.littlekt.graph.node.resource.NinePatchDrawable
import com.lehaine.littlekt.graph.node.resource.VAlign
import com.lehaine.littlekt.graph.node.ui.*
import com.lehaine.littlekt.graph.sceneGraph
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.g2d.NinePatch
import com.lehaine.littlekt.graphics.g2d.SpriteBatch
import com.lehaine.littlekt.graphics.g2d.TextureAtlas
import com.lehaine.littlekt.graphics.g2d.font.BitmapFont
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.math.clamp
import com.lehaine.littlekt.samples.game.Assets
import com.lehaine.littlekt.samples.game.common.GameScene
import com.lehaine.littlekt.util.viewport.ExtendViewport
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
    private val pixelFont: BitmapFont get() = Assets.pixelFont

    private var health = 100f
    private var mana = 100f

    private val graph =
        sceneGraph(context, ExtendViewport(480, 270), batch = batch) {
            textureRect {
                slice = atlas.getByPrefix("rpgBackground").slice
            }
            paddedContainer {
                name = "GUI"
                padding(5)
                anchorRight = 1f
                anchorBottom = 1f

                hBoxContainer {
                    vBoxContainer {
                        name = "Player Info"
                        separation = 5
                        horizontalSizeFlags = Control.SizeFlag.FILL or Control.SizeFlag.EXPAND

                        hBoxContainer {
                            name = "Health Bar"

                            textureProgress {
                                background = atlas.getByPrefix("healthBarBg").slice
                                progressBar = atlas.getByPrefix("healthBarProgress").slice
                                foreground = atlas.getByPrefix("healthBarFg").slice

                                value = health

                                onUpdate += {
                                    value = health
                                }
                            }
                            centerContainer {
                                ninePatchRect {
                                    texture = atlas.getByPrefix("panel9").slice
                                    left = 3
                                    right = 3
                                    top = 3
                                    bottom = 3

                                    minHeight = 25f
                                    minWidth = 50f

                                    label {
                                        font = pixelFont
                                        horizontalAlign = HAlign.CENTER
                                        verticalAlign = VAlign.CENTER
                                        anchorRight = 1f
                                        anchorBottom = 1f

                                        onUpdate += {
                                            text = "HP: ${health.toInt()}"
                                        }
                                    }
                                }
                            }
                        }

                        hBoxContainer {
                            name = "Mana Bar"

                            textureProgress {
                                background = atlas.getByPrefix("manaBarBg").slice
                                progressBar = atlas.getByPrefix("manaBarProgress").slice
                                foreground = atlas.getByPrefix("manaBarFg").slice

                                value = mana

                                onUpdate += {
                                    value = mana
                                }
                            }

                            centerContainer {
                                ninePatchRect {
                                    texture = atlas.getByPrefix("panel9").slice
                                    left = 3
                                    right = 3
                                    top = 3
                                    bottom = 3

                                    minHeight = 25f
                                    minWidth = 50f

                                    label {
                                        font = pixelFont
                                        horizontalAlign = HAlign.CENTER
                                        verticalAlign = VAlign.CENTER
                                        anchorRight = 1f
                                        anchorBottom = 1f

                                        onUpdate += {
                                            text = "MP: ${mana.toInt()}"
                                        }
                                    }
                                }
                            }
                        }

                        ninePatchRect {
                            horizontalSizeFlags = Control.SizeFlag.NONE
                            texture = atlas.getByPrefix("panel9").slice
                            left = 3
                            right = 3
                            top = 3
                            bottom = 3

                            minHeight = 10f
                            minWidth = 50f

                            textureRect {
                                slice = atlas.getByPrefix("gp").slice
                                y -= 3f
                            }

                            label {
                                font = pixelFont
                                text = "148"
                                horizontalAlign = HAlign.CENTER
                                verticalAlign = VAlign.CENTER
                                anchorLeft = 0.5f
                                anchorRight = 1f
                                anchorBottom = 1f

                            }
                        }
                    }

                    vBoxContainer {
                        name = "Zone Info"
                        separation = 5

                        label {
                            font = pixelFont
                            text = "Emerald Forest"
                        }

                        label {
                            font = pixelFont
                            text = "Levels 1-10"
                        }
                    }
                }

                control {
                    panelContainer {
                        panel = NinePatchDrawable(NinePatch(atlas.getByPrefix("panel9").slice, 3, 3, 3, 3))
                        minHeight = 65f
                        anchorTop = 0.75f
                        anchorRight = 1f

                        paddedContainer {
                            padding(7)
                            label {
                                text =
                                    "Press Z and X to decrexase / increase health bar.\nPress C and V to decrease / increase mana bar."
                                font = pixelFont
                            }

                        }
                    }
                }
            }
        }


    override suspend fun Context.show() {
        graph.initialize()
        graph.resize(graphics.width, graphics.height, true)
        graph.root.enabled = true
    }

    override fun Context.render(dt: Duration) {
        gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
        gl.clearColor(Color.DARK_GRAY)
        graph.update(dt)
        graph.render()

        if (input.isKeyPressed(Key.Z)) {
            health--
        } else if (input.isKeyPressed(Key.X)) {
            health++
        }

        if (input.isKeyPressed(Key.C)) {
            mana--
        } else if (input.isKeyPressed(Key.V)) {
            mana++
        }
        health = health.clamp(0f, 100f)
        mana = mana.clamp(0f, 100f)
    }

    override fun Context.resize(width: Int, height: Int) {
        graph.resize(width, height, true)
    }

    override suspend fun Context.hide() {
        updateComponents.clear()
        graph.root.enabled = false
        graph.releaseFocus()
    }
}