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
import net.ccbluex.liquidbounce.cosmetics.render.CosmeticGeometry;
import net.ccbluex.liquidbounce.cosmetics.render.CosmeticMesh;

import java.util.Locale;

public final class HeadwearCosmetic {

    private HeadwearCosmetic() {}

    private static float glowIntensity = 1.0f;

    public static void generateMesh(CosmeticMesh mesh, OutfitConfig outfit, float timeSeconds) {
        int a = CosmeticMesh.toOpaque(outfit.primaryColor());
        int b = CosmeticMesh.toOpaque(outfit.secondaryColor());
        glowIntensity = outfit.glowFactor() * 2.0f;

        mesh.push();
        mesh.translate(0, -outfit.verticalOffset(), 0);

        switch (outfit.styleId().toLowerCase(Locale.ROOT)) {
            case "crown" -> buildCrown(mesh, a, b, timeSeconds);
            case "tophat" -> buildTopHat(mesh, a, b, timeSeconds);
            case "beanie" -> buildBeanie(mesh, a, b, timeSeconds);
            case "horns" -> buildHorns(mesh, a, b, timeSeconds);
            case "wizard" -> buildWizard(mesh, a, b, timeSeconds);
            default -> {}
        }

        mesh.pop();
        mesh.setPen(CosmeticMesh.SOLID);
    }

    private static float hashNoise(float x, float y, float z) {
        int h = Float.floatToIntBits(x * 7.31f + y * 3.17f + z * 5.03f) * 0x27D4EB2D;
        h ^= h >>> 15;
        return ((h & 0xFF) / 255.0f - 0.5f) * 0.12f;
    }

    private static void emitSolidBlock(CosmeticMesh mesh, float x0, float y0, float z0, float x1, float y1, float z1, int color) {
        int top = CosmeticMesh.shadeColor(color, 1.08f + hashNoise(x0, y0, z0));
        int side = CosmeticMesh.shadeColor(color, 1.0f + hashNoise(x1, y0, z1));
        int bottom = CosmeticMesh.shadeColor(color, 0.78f);
        mesh.addBox(x0, y0, z0, x1, y1, z1, top, side, bottom);
    }

    private static void emitHalo(CosmeticMesh mesh, float x, float y, float z, float radius, int color, float alpha) {
        float k = alpha * glowIntensity;
        if (k <= 0.01f) {
            return;
        }
        mesh.addHalo(x, y, z, radius * 1.1f, CosmeticMesh.applyAlpha(CosmeticMesh.blendColor(color, 0xFFFFFFFF, 0.35f), Math.min(1.0f, k)));
        mesh.addHalo(x, y, z, radius * 2.6f, CosmeticMesh.applyAlpha(color, Math.min(1.0f, k * 0.45f)));
    }

    private static void emitGem(CosmeticMesh mesh, float x, float y, float z, float size, int color, float pulse) {
        mesh.setPen(CosmeticMesh.BRIGHT);
        emitSolidBlock(mesh, x - size, y - size, z - size, x + size, y + size, z + size, CosmeticMesh.blendColor(color, 0xFFFFFFFF, 0.15f));
        mesh.setPen(CosmeticMesh.SOLID);
        emitHalo(mesh, x, y, z, size * 5.5f, color, 0.55f * pulse);
    }

    private static void emitSparkle(CosmeticMesh mesh, float x, float y, float z, float size, int color, float fade) {
        if (fade <= 0.02f) {
            return;
        }
        mesh.setPen(CosmeticMesh.BRIGHT);
        float k = size * fade;
        mesh.addBox(x - k, y - k, z - k, x + k, y + k, z + k, CosmeticMesh.blendColor(color, 0xFFFFFFFF, 0.5f));
        mesh.setPen(CosmeticMesh.SOLID);
        emitHalo(mesh, x, y, z, k * 5.0f, color, 0.5f * fade);
    }

    private static float computePulse(float t, float offset) {
        return 0.7f + 0.3f * CosmeticGeometry.sin(t * 2.4f + offset);
    }

    private static void buildCrown(CosmeticMesh mesh, int gold, int gem, float time) {
        float r = 4.45f;
        int rim = CosmeticMesh.shadeColor(gold, 0.88f);
        emitSolidBlock(mesh, -r, -9.6f, -r, r, -8.1f, r, rim);

        for (int side = 0; side < 4; side++) {
            mesh.push();
            mesh.rotateY(side * (float) (Math.PI / 2));
            emitSolidBlock(mesh, -r, -12.0f, -r, -r + 1.25f, -9.6f, -r + 1.25f, gold);
            emitSolidBlock(mesh, -0.7f, -12.8f, -r, 0.7f, -9.6f, -r + 1.25f, gold);
            emitSolidBlock(mesh, -0.45f, -13.4f, -r, 0.45f, -12.8f, -r + 1.0f, gold);
            emitGem(mesh, 0, -10.5f, -r - 0.2f, 0.45f, gem, computePulse(time, side * 0.7f));
            mesh.pop();
        }
    }

    private static void buildTopHat(CosmeticMesh mesh, int body, int band, float time) {
        mesh.addDisc(-8.05f, 6.4f, 32, body, true);
        mesh.addDisc(-8.0f, 6.4f, 32, CosmeticMesh.shadeColor(body, 0.85f), false);

        float[] yLevels = {-8.0f, -9.5f, -12.5f, -15.0f};
        float[] radii = {4.3f, 4.25f, 4.35f, 4.45f};
        float[] zeroOffsets = {0, 0, 0, 0};
        int[] colors = {band, band, body, body};

        CosmeticGeometry.buildLoft(mesh, yLevels, radii, zeroOffsets, zeroOffsets, colors, 24, 2.6f, 0);
        mesh.addDisc(-15.0f, 4.45f, 24, CosmeticMesh.shadeColor(body, 1.1f), true);

        for (int i = 0; i < 6; i++) {
            float phase = (time * 0.4f + i * 0.17f) % 1.0f;
            float angle = i * 1.05f + time * 0.2f;
            float px = CosmeticGeometry.cos(angle) * (5.5f + phase * 2.0f);
            float pz = CosmeticGeometry.sin(angle) * (5.5f + phase * 2.0f);
            float py = -8.0f - phase * 7.5f;
            emitSparkle(mesh, px, py, pz, 0.25f, band, 1.0f - phase);
        }
    }

    private static void buildBeanie(CosmeticMesh mesh, int knit, int cuff, float time) {
        float[] yLevels = {-7.8f, -9.2f, -11.2f, -13.2f, -14.8f};
        float[] radii = {4.6f, 4.5f, 4.2f, 3.0f, 1.0f};
        float[] zeroOffsets = {0, 0, 0, 0, 0};
        int[] colors = {cuff, cuff, knit, knit, knit};

        CosmeticGeometry.buildLoft(mesh, yLevels, radii, zeroOffsets, zeroOffsets, colors, 24, 2.4f, 0.12f);

        mesh.setPen(CosmeticMesh.BRIGHT);
        CosmeticGeometry.buildSphere(mesh, 0, -15.6f, 0, 1.3f, cuff, 8, 10);
        mesh.setPen(CosmeticMesh.SOLID);
    }

    private static void buildHorns(CosmeticMesh mesh, int horn, int glow, float time) {
        for (int side = -1; side <= 1; side += 2) {
            mesh.push();
            mesh.translate(side * 3.4f, -7.8f, -1.5f);

            int segments = 8;
            for (int i = 0; i < segments; i++) {
                float frac = (float) i / segments;
                float r = 0.9f * (1.0f - frac * 0.7f);
                float y = -frac * 6.0f;
                float x = side * (float) Math.pow(frac, 1.3) * 3.0f;
                float z = -frac * 2.0f;

                int col = CosmeticMesh.blendColor(horn, glow, frac * 0.8f);
                mesh.setPen(CosmeticMesh.SOLID);
                emitSolidBlock(mesh, x - r, y - r, z - r, x + r, y + r, z + r, col);

                if (i >= segments - 2) {
                    emitHalo(mesh, x, y, z, 1.5f, glow, 0.6f + 0.4f * (float) Math.sin(time * 3.0f));
                }
            }
            mesh.pop();
        }
    }

    private static void buildWizard(CosmeticMesh mesh, int cloth, int star, float time) {
        mesh.addDisc(-8.05f, 7.2f, 28, cloth, true);
        mesh.addDisc(-8.0f, 7.2f, 28, CosmeticMesh.shadeColor(cloth, 0.85f), false);

        int steps = 10;
        for (int i = 0; i < steps; i++) {
            float f0 = (float) i / steps;
            float f1 = (float) (i + 1) / steps;
            float r0 = 4.3f * (1.0f - f0);
            float r1 = 4.3f * (1.0f - f1);
            float y0 = -8.0f - f0 * 11.5f;
            float y1 = -8.0f - f1 * 11.5f;
            float tilt0 = (float) Math.pow(f0, 2.0) * 2.5f;
            float tilt1 = (float) Math.pow(f1, 2.0) * 2.5f;

            mesh.setPen(CosmeticMesh.SOLID);
            mesh.addBox(-r0, y1, -r0 + tilt0, r0, y0, r0 + tilt1, cloth);
        }

        emitGem(mesh, 2.5f, -19.8f, 0, 0.6f, star, computePulse(time, 0));
    }
}

