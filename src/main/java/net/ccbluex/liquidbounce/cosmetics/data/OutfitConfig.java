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

import java.util.Locale;

/**
 * Representation of an individual cosmetic appearance configuration (style ID, primary/secondary tint, vertical offset, and glow intensity).
 */
public record OutfitConfig(
    String styleId,
    int primaryColor,
    int secondaryColor,
    float verticalOffset,
    float glowFactor
) {

    public static final OutfitConfig EMPTY = new OutfitConfig("", 0, 0, 0.0f, 0.5f);
    public static final float MAX_OFFSET = 4.0f;
    public static final float DEFAULT_GLOW = 0.5f;

    public OutfitConfig(String styleId, int primaryColor, int secondaryColor) {
        this(styleId, primaryColor, secondaryColor, 0.0f, DEFAULT_GLOW);
    }

    public boolean isEmpty() {
        return styleId == null || styleId.isEmpty();
    }

    public static OutfitConfig fromString(String raw, CosmeticPreset.Entry[] availableEntries) {
        if (raw == null || raw.isEmpty()) {
            return EMPTY;
        }

        String[] tokens = raw.split(":");
        CosmeticPreset.Entry found = CosmeticPreset.lookup(availableEntries, tokens[0]);
        if (found == null) {
            return EMPTY;
        }

        int primary = tokens.length > 1 ? parseHex(tokens[1], found.defaultPrimary()) : found.defaultPrimary();
        int secondary = tokens.length > 2 ? parseHex(tokens[2], found.defaultSecondary()) : found.defaultSecondary();
        float offset = tokens.length > 3 ? parseNumber(tokens[3], 0) / 2.0f : 0.0f;
        float glow = tokens.length > 4 ? parseNumber(tokens[4], 5) / 10.0f : DEFAULT_GLOW;

        float clampedOffset = Math.max(-MAX_OFFSET, Math.min(MAX_OFFSET, offset));
        float clampedGlow = Math.max(0.0f, Math.min(1.0f, glow));

        return new OutfitConfig(found.id(), primary, secondary, clampedOffset, clampedGlow);
    }

    private static int parseNumber(String val, int fallback) {
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int parseHex(String val, int fallback) {
        try {
            return Integer.parseInt(val, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public String serialize() {
        if (isEmpty()) {
            return "";
        }
        String base = String.format(Locale.ROOT, "%s:%06x:%06x", styleId, primaryColor & 0xFFFFFF, secondaryColor & 0xFFFFFF);
        int halves = Math.round(verticalOffset * 2.0f);
        int tenths = Math.round(glowFactor * 10.0f);

        if (tenths != Math.round(DEFAULT_GLOW * 10.0f)) {
            return base + ":" + halves + ":" + tenths;
        }
        return halves == 0 ? base : base + ":" + halves;
    }

    public OutfitConfig withPrimaryColor(int newColor) {
        return new OutfitConfig(styleId, newColor, secondaryColor, verticalOffset, glowFactor);
    }

    public OutfitConfig withSecondaryColor(int newColor) {
        return new OutfitConfig(styleId, primaryColor, newColor, verticalOffset, glowFactor);
    }
}

