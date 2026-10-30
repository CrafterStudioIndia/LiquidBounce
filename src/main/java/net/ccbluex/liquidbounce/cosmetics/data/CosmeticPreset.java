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

package net.ccbluex.liquidbounce.cosmetics.data;

/**
 * Built-in registry of wearable cosmetics, auras, wings, and hats.
 */
public final class CosmeticPreset {

    public record Entry(
        String id,
        String displayName,
        int defaultPrimary,
        int defaultSecondary,
        String primaryColorName,
        String secondaryColorName
    ) {}

    public static final Entry[] HEADWEAR = {
        new Entry("tophat", "Top Hat", 0x1C1C20, 0x8E2A35, "Hat", "Band"),
        new Entry("crown", "Royal Crown", 0xF2C14E, 0xD7263D, "Gold", "Gems"),
        new Entry("beanie", "Warm Beanie", 0x3D5A80, 0xE0FBFC, "Knit", "Cuff"),
        new Entry("horns", "Demon Horns", 0x2B1B17, 0xFF4A2E, "Horn", "Glow"),
        new Entry("wizard", "Wizard Hat", 0x3A2E6E, 0xF4D35E, "Hat", "Stars")
    };

    public static final Entry[] WINGS = {
        new Entry("angel", "Archangel", 0xF7F7FA, 0xC9D6EA, "Feathers", "Tips"),
        new Entry("raven", "Corvus", 0x18181D, 0x3B3363, "Feathers", "Sheen"),
        new Entry("phoenix", "Solar Phoenix", 0xFF5A1F, 0xFFD24A, "Flame", "Tips"),
        new Entry("demon", "Abyssal Demon", 0x5A0F16, 0x1A0B0D, "Skin", "Bones"),
        new Entry("dragon", "Ender Drake", 0x2D6A4F, 0x173A2B, "Skin", "Bones"),
        new Entry("bat", "Nocturnal Bat", 0x2A2A2E, 0x121214, "Skin", "Bones"),
        new Entry("butterfly", "Monarch Wings", 0x3A86FF, 0x0B0B12, "Wings", "Edges"),
        new Entry("fairy", "Ether Fairy", 0xBDE8FF, 0xF6F1FF, "Wings", "Veins"),
        new Entry("crystal", "Prismatic Crystal", 0x7FDBFF, 0xC77DFF, "Crystal", "Glow"),
        new Entry("mech", "Cybernetic Thruster", 0x9AA1A9, 0xC9862E, "Plates", "Joints")
    };

    public static final Entry[] AURAS = {
        new Entry("orbit", "Celestial Orbit", 0x9BE7FF, 0xFFFFFF, "Glow", "Core"),
        new Entry("halo", "Divine Halo", 0xFFE08A, 0xFFFFFF, "Glow", "Core"),
        new Entry("embers", "Volcanic Embers", 0xFF7A2F, 0xFFD166, "Glow", "Sparks"),
        new Entry("rings", "Planetary Rings", 0xB794F6, 0xFFFFFF, "Glow", "Core"),
        new Entry("spiral", "Vortex Spiral", 0x6EE7B7, 0xFFFFFF, "Glow", "Core"),
        new Entry("drift", "Ether Drift", 0x8FD3FF, 0xF2F4FF, "Motes", "Ring"),
        new Entry("prism", "Spectral Prism", 0xFF6B9A, 0x7AF0FF, "Ring", "Fade")
    };

    private CosmeticPreset() {}

    public static Entry lookup(Entry[] entries, String id) {
        if (id == null) {
            return null;
        }
        for (Entry entry : entries) {
            if (entry.id().equalsIgnoreCase(id)) {
                return entry;
            }
        }
        return null;
    }

    public static int getIndex(Entry[] entries, String id) {
        if (id == null) {
            return -1;
        }
        for (int i = 0; i < entries.length; i++) {
            if (entries[i].id().equalsIgnoreCase(id)) {
                return i;
            }
        }
        return -1;
    }
}

