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

package net.ccbluex.liquidbounce.cosmetics.features;

import net.ccbluex.liquidbounce.cosmetics.data.OutfitConfig;
import net.ccbluex.liquidbounce.cosmetics.render.CosmeticMesh;
import net.ccbluex.liquidbounce.cosmetics.render.CosmeticPixelBuffer;

import java.util.HashMap;
import java.util.Map;

public final class WingCosmetic {

    private static final int PAL_LIGHT = 1;
    private static final int PAL_BASE = 2;
    private static final int PAL_DARK = 3;
    private static final int PAL_MID = 4;
    private static final int PAL_ACCENT = 5;
    private static final int PAL_BONE = 6;
    private static final int PAL_CLEAR = 7;
    private static final int PAL_SHINE = 8;
    private static final int PAL_HOT = 9;
    private static final int PAL_SHADE = 10;

    private static final Map<String, CosmeticPixelBuffer> PIXEL_BUFFERS = new HashMap<>();
    private static final int[] PALETTE = new int[11];
    private static final byte[] PENS = new byte[11];

    private record WingGeometry(
        float cellSize,
        float depth,
        int rootY,
        int wristX,
        float restAngle,
        float openAngle,
        float beatRate,
        float tiltAngle
    ) {}

    private WingCosmetic() {}

    private static WingGeometry queryGeometry(String style) {
        return switch (style) {
            case "angel", "raven" -> new WingGeometry(0.64f, 0.72f, 6, 14, 0.62f, 0.22f, 1.0f, 0.12f);
            case "phoenix" -> new WingGeometry(0.64f, 0.72f, 6, 14, 0.60f, 0.20f, 1.15f, 0.12f);
            case "demon", "bat" -> new WingGeometry(0.62f, 0.52f, 5, 13, 0.64f, 0.24f, 1.0f, 0.10f);
            case "dragon" -> new WingGeometry(0.68f, 0.58f, 5, 13, 0.64f, 0.24f, 0.88f, 0.10f);
            case "butterfly" -> new WingGeometry(0.72f, 0.42f, 12, 30, 0.56f, 0.30f, 2.1f, 0.05f);
            case "fairy" -> new WingGeometry(0.68f, 0.38f, 14, 30, 0.52f, 0.28f, 2.5f, 0.05f);
            case "crystal" -> new WingGeometry(0.82f, 0.92f, 10, 30, 0.56f, 0.30f, 0.75f, 0.08f);
            case "mech" -> new WingGeometry(0.62f, 0.95f, 4, 13, 0.62f, 0.22f, 0.82f, 0.12f);
            default -> null;
        };
    }

    public static void generateMesh(CosmeticMesh mesh, OutfitConfig outfit, float timeSeconds, float animationPhase, float animationPace) {
        if (outfit.isEmpty()) {
            return;
        }

        String style = outfit.styleId();
        WingGeometry geometry = queryGeometry(style);
        if (geometry == null) {
            return;
        }

        CosmeticPixelBuffer buffer = PIXEL_BUFFERS.computeIfAbsent(style, WingCosmetic::generateBuffer);
        setupPalette(style, CosmeticMesh.toOpaque(outfit.primaryColor()), CosmeticMesh.toOpaque(outfit.secondaryColor()));

        float beat = animationPhase * geometry.beatRate;
        float wave = (float) Math.sin(beat);
        float lag = (float) Math.sin(beat - 0.9f);
        float amplitude = 0.06f + 0.32f * animationPace;
        float sweep = geometry.restAngle + (geometry.openAngle - geometry.restAngle) * animationPace + amplitude * 0.5f * wave;
        float up = 0.08f + 0.14f * animationPace + amplitude * wave;
        float fold = (0.28f - 0.22f * animationPace) + amplitude * 0.7f * lag;
        float breathe = 0.02f * (float) Math.sin(timeSeconds * 1.3f);

        float cell = geometry.cellSize;
        float originY = -geometry.rootY * cell;

        for (int sideSign = -1; sideSign <= 1; sideSign += 2) {
            mesh.push();
            mesh.translate(sideSign * 1.25f, 2.4f, 2.3f);
            mesh.rotateX(-geometry.tiltAngle);
            mesh.rotateZ(-sideSign * (up + breathe));
            mesh.rotateY(-sideSign * sweep);

            buffer.rasterizeToMesh(mesh, PALETTE, PENS, cell, geometry.depth, 0, geometry.wristX, 0, originY, sideSign);

            float hinge = geometry.wristX * cell;
            mesh.translate(sideSign * hinge, 0, 0);
            mesh.rotateY(-sideSign * fold);
            mesh.rotateZ(-sideSign * fold * 0.25f);
            mesh.translate(-sideSign * hinge, 0, 0);

            buffer.rasterizeToMesh(mesh, PALETTE, PENS, cell, geometry.depth, geometry.wristX, buffer.width, 0, originY, sideSign);
            mesh.pop();
        }
    }

    private static void setupPalette(String style, int primary, int secondary) {
        for (int i = 0; i < PENS.length; i++) {
            PENS[i] = CosmeticMesh.SOLID;
        }

        PALETTE[PAL_LIGHT] = CosmeticMesh.shadeColor(primary, 1.18f);
        PALETTE[PAL_BASE] = primary;
        PALETTE[PAL_DARK] = CosmeticMesh.shadeColor(primary, 0.70f);
        PALETTE[PAL_MID] = CosmeticMesh.shadeColor(primary, 0.88f);
        PALETTE[PAL_ACCENT] = secondary;
        PALETTE[PAL_BONE] = secondary;
        PALETTE[PAL_SHADE] = CosmeticMesh.shadeColor(secondary, 0.72f);
        PALETTE[PAL_CLEAR] = CosmeticMesh.applyAlpha(primary, 0.65f);
        PALETTE[PAL_SHINE] = CosmeticMesh.applyAlpha(CosmeticMesh.blendColor(primary, 0xFFFFFFFF, 0.6f), 0.85f);
        PALETTE[PAL_HOT] = CosmeticMesh.blendColor(secondary, 0xFFFFFFFF, 0.4f);

        switch (style) {
            case "phoenix" -> {
                PENS[PAL_LIGHT] = CosmeticMesh.BRIGHT;
                PENS[PAL_HOT] = CosmeticMesh.BRIGHT;
                PENS[PAL_ACCENT] = CosmeticMesh.BRIGHT;
            }
            case "fairy" -> {
                PENS[PAL_CLEAR] = CosmeticMesh.CLEAR;
                PENS[PAL_SHINE] = CosmeticMesh.CLEAR;
                PENS[PAL_MID] = CosmeticMesh.CLEAR;
                PENS[PAL_BASE] = CosmeticMesh.CLEAR;
            }
            case "crystal" -> {
                PENS[PAL_CLEAR] = CosmeticMesh.CLEAR;
                PENS[PAL_SHINE] = CosmeticMesh.CLEAR;
                PENS[PAL_LIGHT] = CosmeticMesh.CLEAR;
                PENS[PAL_BASE] = CosmeticMesh.CLEAR;
                PENS[PAL_HOT] = CosmeticMesh.BRIGHT;
            }
            default -> {}
        }
    }

    private static CosmeticPixelBuffer generateBuffer(String style) {
        return switch (style) {
            case "angel" -> createFeathered(false, false);
            case "raven" -> createFeathered(true, false);
            case "phoenix" -> createFeathered(true, true);
            case "demon" -> createMembrane(5, 1.05f, false);
            case "dragon" -> createMembrane(6, 1.20f, true);
            case "bat" -> createMembrane(5, 0.98f, false);
            case "butterfly" -> createButterfly();
            case "fairy" -> createFairy();
            case "crystal" -> createCrystal();
            case "mech" -> createMech();
            default -> new CosmeticPixelBuffer(1, 1);
        };
    }

    /**
     * Enhanced Feathered Wings: 9 primary flight feathers + inner covert feather tier for majestic silhouette.
     */
    private static CosmeticPixelBuffer createFeathered(boolean ragged, boolean flame) {
        CosmeticPixelBuffer buffer = new CosmeticPixelBuffer(36, 32);
        // Primary humerus and radius wing bones
        buffer.drawCapsule(2, 6, 13, 3, 2.4f, 1.8f, (t, d) -> PAL_BONE);
        buffer.drawCapsule(13, 3, 28, 8, 1.8f, 1.0f, (t, d) -> PAL_BONE);

        int body = ragged ? PAL_DARK : PAL_BASE;

        // Long primary flight feathers
        drawFeatherTip(buffer, 4, 8, 1.62f, 11, 1.6f, flame, body);
        drawFeatherTip(buffer, 8, 8, 1.52f, 13, 1.7f, flame, body);
        drawFeatherTip(buffer, 12, 7, 1.42f, 16, 1.8f, flame, body);
        drawFeatherTip(buffer, 16, 7, 1.32f, 19, 1.9f, flame, body);
        drawFeatherTip(buffer, 20, 7, 1.25f, 22, 1.9f, flame, body);
        drawFeatherTip(buffer, 24, 7, 1.18f, 21, 1.7f, flame, body);
        drawFeatherTip(buffer, 27, 8, 1.05f, 18, 1.5f, flame, body);
        drawFeatherTip(buffer, 30, 9, 0.88f, 15, 1.3f, flame, body);

        // Secondary inner covert layer (shorter overlying feathers)
        drawFeatherTip(buffer, 6, 6, 1.55f, 7, 1.3f, flame, PAL_MID);
        drawFeatherTip(buffer, 11, 5, 1.45f, 9, 1.4f, flame, PAL_MID);
        drawFeatherTip(buffer, 16, 5, 1.35f, 11, 1.4f, flame, PAL_MID);
        drawFeatherTip(buffer, 21, 5, 1.25f, 11, 1.3f, flame, PAL_MID);

        // Bone highlight trim
        buffer.drawCapsule(3, 7, 13, 4, 1.7f, 1.2f, (t, d) -> PAL_LIGHT);
        buffer.drawCapsule(13, 4, 27, 9, 1.2f, 0.8f, (t, d) -> PAL_LIGHT);
        return buffer;
    }

    private static void drawFeatherTip(
        CosmeticPixelBuffer buffer,
        float bx, float by,
        float angle,
        float len,
        float radius,
        boolean flame,
        int body
    ) {
        float ex = bx + (float) Math.cos(angle) * len;
        float ey = by + (float) Math.sin(angle) * len;
        buffer.drawCapsule(bx, by, ex, ey, radius, 0.55f, (t, d) ->
            flame ? (t > 0.65f ? PAL_HOT : (t > 0.32f ? PAL_ACCENT : PAL_BASE))
                  : (t > 0.72f ? PAL_ACCENT : body)
        );
    }

    /**
     * Enhanced Membrane Wings: Articulated knuckle spur, extended trailing web, and sharp talons.
     */
    private static CosmeticPixelBuffer createMembrane(int fingers, float scale, boolean hasSpikes) {
        CosmeticPixelBuffer buffer = new CosmeticPixelBuffer(36, 34);
        float bx = 3, by = 6, wx = 13 * scale, wy = 3;
        buffer.drawCapsule(bx, by, wx, wy, 2.5f, 1.9f, (t, d) -> PAL_BONE);

        float[][] tips = new float[fingers][2];
        for (int i = 0; i < fingers; i++) {
            float frac = (float) i / (fingers - 1);
            float a = 0.52f + frac * 1.12f;
            float len = (20.0f + 9.0f * (1.0f - frac)) * scale;
            tips[i][0] = wx + (float) Math.cos(a) * len;
            tips[i][1] = wy + (float) Math.sin(a) * len;
        }

        // Webbing with scalloped bottom edge
        for (int i = 0; i < fingers - 1; i++) {
            float[] t0 = tips[i];
            float[] t1 = tips[i + 1];
            buffer.drawTriangle(wx, wy, t0[0], t0[1], t1[0], t1[1], (u, v) -> PAL_BASE);

            float mx = (t0[0] + t1[0]) * 0.5f - (wx - (t0[0] + t1[0]) * 0.5f) * 0.09f;
            float my = (t0[1] + t1[1]) * 0.5f - 3.2f;
            buffer.drawTriangle(t0[0], t0[1], t1[0], t1[1], mx, my, (u, v) -> 0);
        }

        float[] lastTip = tips[fingers - 1];
        buffer.drawTriangle(bx, by + 4, wx, wy, lastTip[0], lastTip[1], (u, v) -> PAL_DARK);

        // Articulated bone fingers and talons
        for (int i = 0; i < fingers; i++) {
            buffer.drawCapsule(wx, wy, tips[i][0], tips[i][1], 1.6f, 0.75f, (t, d) -> PAL_BONE);
            // Sharp claw tip
            float clawAngle = (float) Math.atan2(tips[i][1] - wy, tips[i][0] - wx) - 0.25f;
            buffer.drawCapsule(tips[i][0], tips[i][1], tips[i][0] + (float) Math.cos(clawAngle) * 2.2f, tips[i][1] + (float) Math.sin(clawAngle) * 2.2f, 0.75f, 0.3f, (t, d) -> PAL_SHADE);
        }

        // Wrist thumb spur claw
        buffer.drawCapsule(wx - 1, wy - 1, wx + 2.0f, wy - 4.5f, 1.5f, 0.4f, (t, d) -> PAL_SHADE);
        if (hasSpikes) {
            buffer.drawCapsule(bx + 2, by + 1, bx + 1, by - 3.0f, 1.2f, 0.3f, (t, d) -> PAL_SHADE);
        }
        return buffer;
    }

    /**
     * Enhanced Butterfly Wings: Swallowtail elongated trailing tails and dual-tone inner lobes.
     */
    private static CosmeticPixelBuffer createButterfly() {
        CosmeticPixelBuffer buffer = new CosmeticPixelBuffer(28, 34);
        // Upper wing lobe
        buffer.drawEllipse(13, 10, 11, 8.5f, -0.28f, (d, a) -> d < 0.85f ? PAL_BASE : PAL_DARK);
        // Lower wing lobe
        buffer.drawEllipse(10, 22, 8.5f, 7.0f, 0.38f, (d, a) -> d < 0.85f ? PAL_BASE : PAL_DARK);
        // Swallowtail tip extending down
        buffer.drawCapsule(9, 27, 7, 33, 1.8f, 0.4f, (t, d) -> PAL_DARK);

        // Radiant accent spots
        buffer.drawEllipse(14, 9, 3.8f, 2.6f, 0, (d, a) -> d < 0.7f ? PAL_ACCENT : 0);
        buffer.drawEllipse(10, 22, 2.6f, 1.9f, 0, (d, a) -> d < 0.7f ? PAL_ACCENT : 0);
        buffer.drawCapsule(2, 13, 11, 13, 2.0f, 1.5f, (t, d) -> PAL_DARK);
        buffer.traceOutline(PAL_BASE, PAL_DARK);
        return buffer;
    }

    /**
     * Enhanced Fairy Wings: Dual gossamer tier with vein structures.
     */
    private static CosmeticPixelBuffer createFairy() {
        CosmeticPixelBuffer buffer = new CosmeticPixelBuffer(28, 34);
        buffer.drawEllipse(14, 10, 12, 7.5f, -0.42f, (d, a) -> d < 0.9f ? (d < 0.32f ? PAL_SHINE : PAL_CLEAR) : 0);
        buffer.drawEllipse(10, 24, 8.5f, 5.5f, 0.48f, (d, a) -> d < 0.9f ? (d < 0.32f ? PAL_SHINE : PAL_CLEAR) : 0);

        // Vein lattice
        buffer.drawCapsule(2, 14, 15, 9, 1.1f, 0.5f, (t, d) -> PAL_ACCENT);
        buffer.drawCapsule(15, 9, 24, 5, 0.5f, 0.3f, (t, d) -> PAL_ACCENT);
        buffer.drawCapsule(15, 9, 22, 14, 0.5f, 0.3f, (t, d) -> PAL_ACCENT);
        buffer.drawCapsule(2, 14, 12, 23, 1.0f, 0.4f, (t, d) -> PAL_ACCENT);
        buffer.drawCapsule(12, 23, 15, 29, 0.4f, 0.2f, (t, d) -> PAL_ACCENT);
        return buffer;
    }

    /**
     * Enhanced Crystal Wings: 7 staggered multi-faceted crystalline blades.
     */
    private static CosmeticPixelBuffer createCrystal() {
        CosmeticPixelBuffer buffer = new CosmeticPixelBuffer(32, 32);
        buffer.drawCapsule(2, 10, 11, 8, 2.6f, 1.9f, (t, d) -> PAL_SHADE);

        float[][] shards = {
            {9, 8, 22, 1, 2.3f},
            {11, 9, 29, 7, 2.5f},
            {12, 10, 28, 14, 2.3f},
            {10, 11, 25, 20, 2.1f},
            {8, 12, 19, 26, 1.9f},
            {6, 13, 14, 30, 1.6f}
        };

        for (float[] shard : shards) {
            buffer.drawCapsule(
                shard[0], shard[1], shard[2], shard[3], shard[4], 0.6f,
                (t, d) -> t > 0.78f ? PAL_HOT : (d < 0.38f ? PAL_SHINE : PAL_CLEAR)
            );
        }
        return buffer;
    }

    /**
     * Enhanced Mech Thrusters: Articulated vectoring pods with heat vents.
     */
    private static CosmeticPixelBuffer createMech() {
        CosmeticPixelBuffer buffer = new CosmeticPixelBuffer(34, 32);
        // Central hydraulic arm
        buffer.drawCapsule(2, 6, 12, 4, 2.8f, 2.3f, (t, d) -> PAL_BONE);
        buffer.drawCapsule(12, 4, 24, 6, 2.2f, 1.5f, (t, d) -> PAL_BONE);
        buffer.drawCapsule(12, 4, 12, 4, 3.4f, 3.4f, (t, d) -> PAL_SHADE);

        // Vectoring thruster fins
        for (int i = 0; i < 6; i++) {
            float frac = (float) i / 5.0f;
            float bx = 6.0f + frac * 15.0f;
            float by = 6.0f + frac * 2.0f;
            float a = 0.92f + frac * 0.28f;
            float len = 13.0f + frac * 6.5f;
            float ex = bx + (float) Math.cos(a) * len;
            float ey = by + (float) Math.sin(a) * len;

            buffer.drawCapsule(bx, by, ex, ey, 1.9f, 0.95f, (t, d) -> t > 0.68f ? PAL_ACCENT : (d < 0.5f ? PAL_LIGHT : PAL_BASE));
        }

        // Exhaust core
        buffer.drawCapsule(12, 4, 12, 4, 1.6f, 1.6f, (t, d) -> PAL_HOT);
        return buffer;
    }
}
