package com.littlekt.samples.game.scenes

import com.littlekt.Context
import com.littlekt.graph.node.resource.NinePatchDrawable
import com.littlekt.graph.node.ui.*
import com.littlekt.graph.sceneGraph
import com.littlekt.graphics.Color
import com.littlekt.graphics.HAlign
import com.littlekt.graphics.VAlign
import com.littlekt.graphics.g2d.NinePatch
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.TextureAtlas
import com.littlekt.graphics.g2d.font.BitmapFont
import com.littlekt.graphics.webgpu.*
import com.littlekt.input.Key
import com.littlekt.math.clamp
import com.littlekt.samples.game.Assets
import com.littlekt.samples.game.common.GameScene
import com.littlekt.util.viewport.ExtendViewport
import com.littlekt.util.viewport.setViewport
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 2/24/2022
 */
class RPGUIScene(
    context: Context
) : GameScene(context) {
    private val atlas: TextureAtlas get() = Assets.atlas
    private val pixelFont: BitmapFont get() = Assets.pixelFont

    private var health = 100f
    private var mana = 100f

   private val device = context.graphics.device
   private val preferredFormat = context.graphics.preferredFormat
   private val batch = SpriteBatch(device, context.graphics, preferredFormat)

    private val graph =
        sceneGraph(context, ExtendViewport(480, 270), batch) {
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
                        horizontalSizing = Control.SizeFlag.FILL or Control.SizeFlag.EXPAND

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
                            horizontalSizing = Control.SizeFlag.NONE
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

    override fun Context.update(dt: Duration) {
        val surfaceTexture = context.graphics.surface.getCurrentTexture()
        when (val status = surfaceTexture.status) {
            TextureStatus.SUCCESS -> {
                // all good, could check for `surfaceTexture.suboptimal` here.
            }

            TextureStatus.TIMEOUT,
            TextureStatus.OUTDATED,
            TextureStatus.LOST -> {
                surfaceTexture.texture?.release()
                logger.info { "getCurrentTexture status=$status" }
                return
            }

            else -> {
                // fatal
                logger.fatal { "getCurrentTexture status=$status" }
                close()
                return
            }
        }
        val swapChainTexture = checkNotNull(surfaceTexture.texture)
        val frame = swapChainTexture.createView()

        val commandEncoder = device.createCommandEncoder()
        val renderPassDescriptor =
            RenderPassDescriptor(
                listOf(
                    RenderPassColorAttachmentDescriptor(
                        view = frame,
                        loadOp = LoadOp.CLEAR,
                        storeOp = StoreOp.STORE,
                        clearColor =
                        if (preferredFormat.srgb) Color.DARK_GRAY.toLinear()
                        else Color.DARK_GRAY
                    )
                ),
                label = "Init render pass"
            )
        graph.update(dt)
        graph.render(commandEncoder, renderPassDescriptor)

        val commandBuffer = commandEncoder.finish()

        device.queue.submit(commandBuffer)
        graphics.surface.present()

        commandBuffer.release()
        commandEncoder.release()
        frame.release()
        swapChainTexture.release()

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
        graphics.configureSurface(
            TextureUsage.RENDER_ATTACHMENT,
            preferredFormat,
            PresentMode.FIFO,
            graphics.surfaceCapabilities.alphaModes[0]
        )
    }

    override suspend fun Context.hide() {
        updateComponents.clear()
        graph.root.enabled = false
        graph.releaseFocus()
    }
}