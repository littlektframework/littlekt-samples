package com.littlekt.samples.game.scenes

import com.littlekt.Context
import com.littlekt.async.KtScope
import com.littlekt.graph.node.ui.button
import com.littlekt.graph.node.ui.centerContainer
import com.littlekt.graph.node.ui.column
import com.littlekt.graph.node.ui.label
import com.littlekt.graph.sceneGraph
import com.littlekt.graphics.Color
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.webgpu.*
import com.littlekt.samples.game.common.GameScene
import com.littlekt.util.viewport.ScreenViewport
import kotlinx.coroutines.launch
import kotlin.reflect.KClass
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 2/24/2022
 */
class SelectionScene(
    private val onSelection: suspend (KClass<out GameScene>) -> Unit,
    context: Context
) : GameScene(context) {
    private val device = context.graphics.device
    private val preferredFormat = context.graphics.preferredFormat
    private val batch = SpriteBatch(device, context.graphics, preferredFormat)
    private val graph = sceneGraph(context, ScreenViewport(960, 540), batch = batch) {
        centerContainer {
            anchorRight = 1f
            anchorTop = 1f
            column {
                separation = 20
                label {
                    text = "Select a Sample:"
                }

                column {
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

    override fun Context.update(dt: Duration) {
        val device = context.graphics.device
        val preferredFormat = context.graphics.preferredFormat

        val surfaceTexture = graphics.surface.getCurrentTexture()
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

        val commandEncoder = device.createCommandEncoder("scenegraph command encoder")
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
        if (batch.drawing) batch.end()

        val commandBuffer = commandEncoder.finish()

        device.queue.submit(commandBuffer)
        graphics.surface.present()

        commandBuffer.release()
        commandEncoder.release()
        frame.release()
        swapChainTexture.release()
    }

    override fun Context.resize(width: Int, height: Int) {
        graph.resize(width, height)
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