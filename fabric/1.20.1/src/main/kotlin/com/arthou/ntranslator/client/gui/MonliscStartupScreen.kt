package com.arthou.ntranslator.client.gui

import com.arthou.ntranslator.NTranslator
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferUploader
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.Util
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvent
import java.time.LocalTime

class MonliscStartupScreen(private val parent: Screen) : Screen(Component.empty()) {
    companion object {
        private const val SESSION_OWNER_PROPERTY = "monlisc.technologies.startup.owner"
        private val IMAGE_LIGHT = NTranslator.id("textures/gui/monlisc_technologies.png")
        private val IMAGE_DARK = NTranslator.id("textures/gui/monlisc_technologies_dark.png")
        private val STARTUP_SOUND = NTranslator.id("monlisc_startup")
        private const val IMAGE_WIDTH = 1600
        private const val IMAGE_HEIGHT = 900
        private const val DISPLAY_TIME_MS = 2400L
        private const val TITLE_SCREEN_DELAY_TICKS = 20

        private var hasShownThisSession = false
        private var titleScreenTicks = 0

        fun maybeOpen(mc: Minecraft) {
            if (hasShownThisSession || isStartupSessionHandled()) {
                hasShownThisSession = true
                return
            }

            val current = mc.screen
            if (isAnyMonliscStartupScreen(current)) {
                rememberExternalStartup()
                hasShownThisSession = true
                titleScreenTicks = 0
                return
            }

            if (current !is TitleScreen) {
                titleScreenTicks = 0
                return
            }

            titleScreenTicks++
            if (titleScreenTicks < TITLE_SCREEN_DELAY_TICKS) {
                return
            }

            if (!claimStartupSession(NTranslator.MOD_ID)) {
                hasShownThisSession = true
                titleScreenTicks = 0
                return
            }

            hasShownThisSession = true
            titleScreenTicks = 0
            mc.setScreen(MonliscStartupScreen(current))
        }

        private fun isAnyMonliscStartupScreen(screen: Screen?): Boolean {
            return screen != null && screen.javaClass.simpleName == MonliscStartupScreen::class.java.simpleName
        }

        private fun isStartupSessionHandled(): Boolean {
            return synchronized(System.getProperties()) {
                !System.getProperty(SESSION_OWNER_PROPERTY).isNullOrBlank()
            }
        }

        private fun claimStartupSession(owner: String): Boolean {
            return synchronized(System.getProperties()) {
                if (!System.getProperty(SESSION_OWNER_PROPERTY).isNullOrBlank()) {
                    false
                } else {
                    System.setProperty(SESSION_OWNER_PROPERTY, owner)
                    true
                }
            }
        }

        private fun rememberExternalStartup() {
            synchronized(System.getProperties()) {
                if (System.getProperty(SESSION_OWNER_PROPERTY).isNullOrBlank()) {
                    System.setProperty(SESSION_OWNER_PROPERTY, "external")
                }
            }
        }
    }

    private var openedAt = 0L
    private var playbackStarted = false
    private var darkTheme = false
    private var startupSound: SimpleSoundInstance? = null

    override fun init() {
        // Resizing must not restart the sound or extend the presentation.
        if (!playbackStarted) {
            darkTheme = LocalTime.now().hour !in 6..17
            playbackStarted = true
            openedAt = Util.getMillis()
            val sound = SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(STARTUP_SOUND), 1.0f, 0.7f)
            startupSound = sound
            minecraft?.soundManager?.play(sound)
        }
    }

    override fun tick() {
        if (playbackStarted && Util.getMillis() - openedAt >= DISPLAY_TIME_MS) {
            onClose()
        }
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val background = if (darkTheme) 0xFF101216.toInt() else 0xFFFFFFFF.toInt()
        val image = if (darkTheme) IMAGE_DARK else IMAGE_LIGHT
        guiGraphics.fill(0, 0, width, height, background)
        val scale = minOf(width.toFloat() / IMAGE_WIDTH, height.toFloat() / IMAGE_HEIGHT) * 0.90f
        val drawWidth = IMAGE_WIDTH * scale
        val drawHeight = IMAGE_HEIGHT * scale
        val left = (width - drawWidth) / 2f
        val top = (height - drawHeight) / 2f
        val right = left + drawWidth
        val bottom = top + drawHeight
        val pose = guiGraphics.pose().last().pose()

        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
        RenderSystem.setShader(GameRenderer::getPositionTexShader)
        RenderSystem.setShaderTexture(0, image)
        val builder = Tesselator.getInstance().builder
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)
        builder.vertex(pose, left, top, 0f).uv(0f, 0f).endVertex()
        builder.vertex(pose, left, bottom, 0f).uv(0f, 1f).endVertex()
        builder.vertex(pose, right, bottom, 0f).uv(1f, 1f).endVertex()
        builder.vertex(pose, right, top, 0f).uv(1f, 0f).endVertex()
        BufferUploader.drawWithShader(builder.end())
        RenderSystem.disableBlend()
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        onClose()
        return true
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        onClose()
        return true
    }

    override fun charTyped(codePoint: Char, modifiers: Int): Boolean {
        onClose()
        return true
    }

    override fun shouldCloseOnEsc(): Boolean = false
    override fun isPauseScreen(): Boolean = false

    override fun removed() {
        stopPlayback()
    }

    override fun onClose() {
        stopPlayback()
        minecraft?.setScreen(parent)
    }

    private fun stopPlayback() {
        startupSound?.let { minecraft?.soundManager?.stop(it) }
        startupSound = null
    }
}
