/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.cosmetics.CosmeticsEngine
import net.ccbluex.liquidbounce.integration.backend.BrowserBackendManager
import net.ccbluex.liquidbounce.integration.screen.impl.InternetExplorerScreen
import net.ccbluex.liquidbounce.utils.client.asPlainText
import net.ccbluex.liquidbounce.utils.client.browseUrl
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.regular
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.CommonComponents

class GuiCapeArtScreen(private val parent: Screen? = null) : Screen("CapeArt Studio & Catalog".asPlainText()) {

    override fun init() {
        val centerX = width / 2
        val startY = height / 4

        val btnWidth = 220
        val btnHeight = 20

        // Button 1: Launch CapeArt Pixel Studio
        addRenderableWidget(
            Button.builder("🎨 Open CapeArt Studio (In-Game)".asPlainText()) {
                if (BrowserBackendManager.backend != null) {
                    mc.setScreen(InternetExplorerScreen("${CosmeticsEngine.getBackendUrl()}/capes#editor"))
                } else {
                    browseUrl("${CosmeticsEngine.getBackendUrl()}/capes#editor")
                    chat(regular("Opened CapeArt Pixel Studio in your browser!"))
                }
            }.bounds(centerX - btnWidth / 2, startY, btnWidth, btnHeight).build()
        )

        // Button 2: Browse Community Cape Catalog
        addRenderableWidget(
            Button.builder("🌐 Browse Cape Catalog & Menu".asPlainText()) {
                if (BrowserBackendManager.backend != null) {
                    mc.setScreen(InternetExplorerScreen("${CosmeticsEngine.getBackendUrl()}/capes#catalog"))
                } else {
                    browseUrl("${CosmeticsEngine.getBackendUrl()}/capes#catalog")
                    chat(regular("Opened Cape Catalog in your browser!"))
                }
            }.bounds(centerX - btnWidth / 2, startY + 26, btnWidth, btnHeight).build()
        )

        // Button 3: Equip Liquid Gold Classic
        addRenderableWidget(
            Button.builder("✨ Equip 'Liquid Gold Classic'".asPlainText()) {
                CosmeticsEngine.equipPublishedCape("1") { success, msg ->
                    chat(regular(if (success) "Equipped Liquid Gold Classic!" else "Failed: $msg"))
                }
            }.bounds(centerX - btnWidth / 2, startY + 52, btnWidth, btnHeight).build()
        )

        // Button 4: Equip Cosmic Nebula HD
        addRenderableWidget(
            Button.builder("🌌 Equip 'Cosmic Nebula HD'".asPlainText()) {
                CosmeticsEngine.equipPublishedCape("2") { success, msg ->
                    chat(regular(if (success) "Equipped Cosmic Nebula HD!" else "Failed: $msg"))
                }
            }.bounds(centerX - btnWidth / 2, startY + 78, btnWidth, btnHeight).build()
        )

        // Back / Close Button
        addRenderableWidget(
            Button.builder(CommonComponents.GUI_BACK) {
                mc.setScreen(parent)
            }.bounds(centerX - btnWidth / 2, startY + 114, btnWidth, btnHeight).build()
        )
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(context, mouseX, mouseY, partialTick)
        val centerX = width / 2
        val startY = height / 4

        context.drawCenteredString(font, title, centerX, startY - 40, 0xFFFFFF)
        context.drawCenteredString(
            font,
            "Design custom & HD capes, submit for admin review, and equip approved capes.".asPlainText(),
            centerX,
            startY - 25,
            0xAAAAAA
        )

        super.render(context, mouseX, mouseY, partialTick)
    }

    override fun isPauseScreen() = false
}

