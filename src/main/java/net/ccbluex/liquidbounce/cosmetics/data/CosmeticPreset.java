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
        new Entry("hat-liquid-cap", "Liquid Cap", 0x0EA5E9, 0x38BDF8, "Cap", "Brim"),
        new Entry("hat-liquid-headband", "Liquid Headband", 0x0EA5E9, 0x38BDF8, "Band", "Sigil"),
        new Entry("golden-halo", "Golden Halo", 0xF2C14E, 0xFFFFFF, "Gold", "Glow"),
        new Entry("crown", "Royal Crown", 0xF2C14E, 0xD7263D, "Gold", "Gems"),
        new Entry("hat-crown", "Royal Crown", 0xF2C14E, 0xD7263D, "Gold", "Gems"),
        new Entry("hat-victorian-crown", "Victorian Crown", 0xE0E7FF, 0xF2C14E, "Silver", "Gems"),
        new Entry("hat-fire-crown", "Fire Crown", 0xFF6B00, 0xFFD24A, "Fire", "Glow"),
        new Entry("hat-goat-horns", "Goat Horns", 0xE2E8F0, 0x94A3B8, "Horn", "Base"),
        new Entry("hat-shadow-horns", "Shadow Horns", 0x1E1E2E, 0x6366F1, "Shadow", "Glow"),
        new Entry("horns", "Demon Horns", 0x2B1B17, 0xFF4A2E, "Horn", "Glow"),
        new Entry("tophat", "Top Hat", 0x1C1C20, 0x8E2A35, "Hat", "Band"),
        new Entry("hat-monocle", "Monocle", 0x1E293B, 0xF2C14E, "Frame", "Glass"),
        new Entry("hat-bucket", "Bucket Hat", 0x334155, 0x64748B, "Cloth", "Brim"),
        new Entry("hat-straw", "Straw Hat", 0xFDE047, 0xEAB308, "Straw", "Band"),
        new Entry("hat-bunny", "Bunny Ears", 0xF472B6, 0xFFFFFF, "Fur", "Inner"),
        new Entry("hat-pumpkin-head", "Pumpkin Head", 0xEA580C, 0x78350F, "Pumpkin", "Stem"),
        new Entry("beanie", "Warm Beanie", 0x3D5A80, 0xE0FBFC, "Knit", "Cuff"),
        new Entry("hat-orange-wool", "Orange Wool", 0xF97316, 0xEA580C, "Wool", "Cuff"),
        new Entry("hat-fiery-headband", "Fiery Headband", 0xFF5722, 0xFFD24A, "Fire", "Band"),
        new Entry("hat-butterfly-halo", "Butterfly Halo", 0x38BDF8, 0xFFFFFF, "Wing", "Halo"),
        new Entry("wizard", "Wizard Hat", 0x3A2E6E, 0xF4D35E, "Hat", "Stars"),
        new Entry("witch_hat", "Witch Hat", 0x4C1D95, 0x8B5CF6, "Hat", "Buckle"),
        new Entry("santa_hat", "Santa Hat", 0xDC2626, 0xFFFFFF, "Fleece", "Trim"),
        new Entry("cat_ears", "Cat Ears", 0x1E293B, 0xF472B6, "Ears", "Lining")
    };

    public static final Entry[] WINGS = {
        new Entry("angel", "Archangel", 0xF7F7FA, 0xC9D6EA, "Feathers", "Tips"),
        new Entry("golden-wings", "Golden Wings", 0xF5BE23, 0xFFF3C4, "Gold", "Feathers"),
        new Entry("wings-angel", "Angel Wings", 0xF7F7FA, 0xC9D6EA, "Feathers", "Tips"),
        new Entry("wings-autumn-breeze", "Autumn Breeze", 0xFB923C, 0xFDE047, "Amber", "Foliage"),
        new Entry("wings-baby-neon-dragon", "Baby Neon Dragon", 0x06B6D4, 0x22D3EE, "Cyan", "Glow"),
        new Entry("wings-baby-white-dragon", "Baby White Dragon", 0xF1F5F9, 0x94A3B8, "Pearl", "Scales"),
        new Entry("wings-fiery-dragon", "Fiery Dragon", 0xFF5A1F, 0xFFD24A, "Flame", "Tips"),
        new Entry("wings-ghost-dragon", "Ghost Dragon", 0xE2E8F0, 0x64748B, "Ether", "Spines"),
        new Entry("wings-pumpkin", "Pumpkin Wings", 0xEA580C, 0x78350F, "Amber", "Vines"),
        new Entry("wings-skeleton", "Skeleton Wings", 0xD1D5DB, 0x4B5563, "Bone", "Joints"),
        new Entry("wings-butterfly", "Monarch Butterfly", 0x3A86FF, 0x0B0B12, "Wings", "Edges"),
        new Entry("phoenix", "Solar Phoenix", 0xFF5A1F, 0xFFD24A, "Flame", "Tips"),
        new Entry("demon", "Abyssal Demon", 0x5A0F16, 0x1A0B0D, "Skin", "Bones"),
        new Entry("dragon", "Ender Drake", 0x2D6A4F, 0x173A2B, "Skin", "Bones"),
        new Entry("raven", "Corvus", 0x18181D, 0x3B3363, "Feathers", "Sheen"),
        new Entry("bat", "Nocturnal Bat", 0x2A2A2E, 0x121214, "Skin", "Bones"),
        new Entry("butterfly", "Monarch Wings", 0x3A86FF, 0x0B0B12, "Wings", "Edges"),
        new Entry("fairy", "Ether Fairy", 0xBDE8FF, 0xF6F1FF, "Wings", "Veins"),
        new Entry("crystal", "Prismatic Crystal", 0x7FDBFF, 0xC77DFF, "Crystal", "Glow"),
        new Entry("mech", "Cybernetic Thruster", 0x9AA1A9, 0xC9862E, "Plates", "Joints")
    };

    public static final Entry[] AURAS = {
        new Entry("aura-liquid-sigil", "Liquid Sigil", 0x0EA5E9, 0x38BDF8, "Cyan", "Sigil"),
        new Entry("aura-autumn-breeze", "Autumn Breeze", 0xFB923C, 0xFDE047, "Amber", "Motes"),
        new Entry("aura-inferno", "Inferno Flame", 0xFF4500, 0xFFD700, "Flame", "Cinders"),
        new Entry("orbit", "Celestial Orbit", 0x9BE7FF, 0xFFFFFF, "Glow", "Core"),
        new Entry("halo", "Divine Halo", 0xFFE08A, 0xFFFFFF, "Glow", "Core"),
        new Entry("embers", "Volcanic Embers", 0xFF7A2F, 0xFFD166, "Glow", "Sparks"),
        new Entry("rings", "Planetary Rings", 0xB794F6, 0xFFFFFF, "Glow", "Core"),
        new Entry("spiral", "Vortex Spiral", 0x6EE7B7, 0xFFFFFF, "Glow", "Core"),
        new Entry("drift", "Ether Drift", 0x8FD3FF, 0xF2F4FF, "Motes", "Ring"),
        new Entry("prism", "Spectral Prism", 0xFF6B9A, 0x7AF0FF, "Ring", "Fade"),
        new Entry("purple_lightning", "Purple Lightning", 0xA855F7, 0xE9D5FF, "Sparks", "Bolt"),
        new Entry("portal", "End Portal", 0x7E22CE, 0xD8B4FE, "Void", "Stars"),
        new Entry("aura-bat", "Bat Swarm", 0x1E1E2E, 0x475569, "Bats", "Shadow"),
        new Entry("aura-blood-rain", "Blood Rain", 0xDC2626, 0xEF4444, "Crimson", "Droplets"),
        new Entry("aura-broken-hearts", "Broken Hearts", 0xBE123C, 0xFB7185, "Heart", "Fragments"),
        new Entry("aura-bubble", "Bubble Motes", 0x38BDF8, 0xE0F2FE, "Bubble", "Shine"),
        new Entry("aura-candle", "Mystic Candle", 0xFDE047, 0xFEF08A, "Flame", "Aura"),
        new Entry("aura-candy", "Sweet Candy", 0xEC4899, 0xFDF2F8, "Candy", "Sugar"),
        new Entry("aura-fish", "Fish Swarm", 0x0EA5E9, 0x67E8F9, "Fish", "Ocean"),
        new Entry("aura-halloween-particle", "Halloween Particles", 0xEA580C, 0x78350F, "Spooks", "Motes"),
        new Entry("aura-heart", "Floating Hearts", 0xEC4899, 0xFFFFFF, "Heart", "Glow")
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

