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

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.cosmetics.data.OutfitConfig
import net.ccbluex.liquidbounce.cosmetics.data.PlayerWardrobe
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import java.util.Locale

object ModuleAuraCosmetic : ClientModule("Aura", ModuleCategories.RENDER) {

    private val style = choices("Style", 0) {
        arrayOf(
            AuraStyle.Orbit,
            AuraStyle.Halo,
            AuraStyle.Embers,
            AuraStyle.Rings,
            AuraStyle.Spiral,
            AuraStyle.Drift,
            AuraStyle.Prism
        )
    }

    private sealed class AuraStyle(name: String) : Mode(name) {
        override val parent: ModeValueGroup<*>
            get() = style

        object Orbit : AuraStyle("Orbit")
        object Halo : AuraStyle("Halo")
        object Embers : AuraStyle("Embers")
        object Rings : AuraStyle("Rings")
        object Spiral : AuraStyle("Spiral")
        object Drift : AuraStyle("Drift")
        object Prism : AuraStyle("Prism")
    }

    private val primaryColor by color("PrimaryColor", Color4b(155, 231, 255, 255))
    private val secondaryColor by color("SecondaryColor", Color4b(255, 255, 255, 255))
    private val lift by float("Lift", 0.0f, -4.0f..4.0f)
    private val glow by float("Glow", 0.5f, 0.0f..1.0f)

    private fun applyAura() {
        if (!running) {
            PlayerWardrobe.setAura(OutfitConfig.EMPTY)
            return
        }
        val s = style.activeMode.name.lowercase(Locale.ROOT)
        val p = primaryColor.argb and 0x00FFFFFF
        val sec = secondaryColor.argb and 0x00FFFFFF
        PlayerWardrobe.setAura(OutfitConfig(s, p, sec, lift, glow))
    }

    override fun onEnabled() {
        applyAura()
    }

    override fun onDisabled() {
        PlayerWardrobe.setAura(OutfitConfig.EMPTY)
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        applyAura()
    }
}
