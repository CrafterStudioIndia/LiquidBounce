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

public final class AuraEffect {

    private AuraEffect() {}

    public static void generateMesh(CosmeticMesh mesh, OutfitConfig outfit, float timeSeconds) {
        if (outfit.isEmpty()) {
            return;
        }

        int glow = CosmeticMesh.toOpaque(outfit.primaryColor());
        int core = CosmeticMesh.toOpaque(outfit.secondaryColor());

        switch (outfit.styleId().toLowerCase(Locale.ROOT)) {
            case "orbit" -> buildOrbit(mesh, glow, core, timeSeconds);
            case "halo" -> buildHalo(mesh, glow, core, timeSeconds);
            case "embers" -> buildEmbers(mesh, glow, core, timeSeconds);
            case "rings" -> buildRings(mesh, glow, core, timeSeconds);
            case "spiral" -> buildSpiral(mesh, glow, core, timeSeconds);
            case "drift" -> buildDrift(mesh, glow, core, core, timeSeconds, false);
            case "prism" -> buildDrift(mesh, 0, glow, core, timeSeconds, true);
            default -> {}
        }
        mesh.setPen(CosmeticMesh.SOLID);
    }

    private static void emitOrb(CosmeticMesh mesh, float x, float y, float z, float radius, int glowColor, int coreColor) {
        mesh.setPen(CosmeticMesh.BRIGHT);
        CosmeticGeometry.buildSphere(mesh, x, y, z, radius, CosmeticMesh.blendColor(coreColor, glowColor, 0.3f), 5, 8);
        mesh.addHalo(x, y, z, radius * 3.8f, CosmeticMesh.applyAlpha(glowColor, 0.75f));
    }

    /**
     * Enhanced Quad-Orbital Resonance: 4 satellites on dual counter-inclined precession orbits.
     */
    private static void buildOrbit(CosmeticMesh mesh, int glow, int core, float time) {
        // 4 satellites across dual planes
        for (int i = 0; i < 4; i++) {
            boolean isUpperPlane = (i % 2 == 0);
            float orbitSpeed = isUpperPlane ? 1.7f : -1.4f;
            float baseAngle = time * orbitSpeed + (i * (float) (Math.PI / 2.0));
            float radius = isUpperPlane ? 11.5f : 13.0f;

            for (int k = 0; k < 9; k++) {
                float trailAngle = baseAngle - k * (isUpperPlane ? 0.12f : -0.12f);
                float x = CosmeticGeometry.cos(trailAngle) * radius;
                float z = CosmeticGeometry.sin(trailAngle) * radius;
                float verticalOscillation = CosmeticGeometry.sin(trailAngle * 2.2f + i * 1.5f) * (isUpperPlane ? 4.5f : 3.5f);
                float y = (isUpperPlane ? 8.0f : 4.0f) + verticalOscillation;
                float fade = 1.0f - (float) k / 9.0f;

                if (k == 0) {
                    emitOrb(mesh, x, y, z, 1.25f, glow, core);
                    // Emit small accent sparkles around leading orb
                    mesh.addHalo(x, y, z, 3.2f, CosmeticMesh.applyAlpha(core, 0.8f));
                } else {
                    mesh.addHalo(x, y, z, 2.4f * fade, CosmeticMesh.applyAlpha(glow, 0.65f * fade));
                }
            }
        }
    }

    private static void buildBand(
        CosmeticMesh mesh,
        float y,
        float radius,
        float width,
        float height,
        int segments,
        int innerColor,
        int outerColor,
        float rotation
    ) {
        for (int i = 0; i < segments; i++) {
            double a = Math.PI * 2 * i / segments + rotation;
            double b = Math.PI * 2 * (i + 1) / segments + rotation;

            float ca = CosmeticGeometry.cos(a), sa = CosmeticGeometry.sin(a);
            float cb = CosmeticGeometry.cos(b), sb = CosmeticGeometry.sin(b);
            float innerR = radius - width / 2.0f;
            float outerR = radius + width / 2.0f;

            mesh.addQuad(
                ca * innerR, y, sa * innerR,
                ca * radius, y, sa * radius,
                cb * radius, y, sb * radius,
                cb * innerR, y, sb * innerR,
                outerColor, innerColor, innerColor, outerColor
            );
            mesh.addQuad(
                ca * radius, y, sa * radius,
                ca * outerR, y, sa * outerR,
                cb * outerR, y, sb * outerR,
                cb * radius, y, sb * radius,
                innerColor, outerColor, outerColor, innerColor
            );

            if (height > 0) {
                mesh.addQuad(
                    ca * radius, y - height / 2.0f, sa * radius,
                    ca * radius, y, sa * radius,
                    cb * radius, y, sb * radius,
                    cb * radius, y - height / 2.0f, sb * radius,
                    outerColor, innerColor, innerColor, outerColor
                );
                mesh.addQuad(
                    ca * radius, y, sa * radius,
                    ca * radius, y + height / 2.0f, sa * radius,
                    cb * radius, y + height / 2.0f, sb * radius,
                    cb * radius, y, sb * radius,
                    innerColor, outerColor, outerColor, innerColor
                );
            }
        }
    }

    private static void buildHoop(CosmeticMesh mesh, float radius, float thickness, int segments, int color) {
        for (int i = 0; i < segments; i++) {
            double a = Math.PI * 2 * i / segments;
            double b = Math.PI * 2 * (i + 1) / segments;

            float ca = CosmeticGeometry.cos(a), sa = CosmeticGeometry.sin(a);
            float cb = CosmeticGeometry.cos(b), sb = CosmeticGeometry.sin(b);
            float r0 = radius - thickness / 2.0f;
            float r1 = radius + thickness / 2.0f;

            mesh.addQuad(
                ca * r0, 0, sa * r0,
                ca * r1, 0, sa * r1,
                cb * r1, 0, sb * r1,
                cb * r0, 0, sb * r0,
                color
            );
        }
    }

    /**
     * Enhanced Crown Halo: Dual-layered concentric halo with rotating floating diamond star motes.
     */
    private static void buildHalo(CosmeticMesh mesh, int glow, int core, float time) {
        mesh.push();
        float floatY = 19.0f + CosmeticGeometry.sin(time * 1.6f) * 0.7f;
        mesh.translate(0, floatY, 0);
        mesh.rotateZ((float) Math.sin(time * 0.85f) * 0.09f);
        mesh.rotateX((float) Math.cos(time * 0.75f) * 0.07f);

        // Inner radiant band
        mesh.setPen(CosmeticMesh.CLEAR);
        buildBand(mesh, 0, 6.2f, 2.2f, 0.0f, 32, CosmeticMesh.applyAlpha(core, 0.9f), CosmeticMesh.applyAlpha(glow, 0.0f), time * 0.4f);

        // Outer crisp halo ring
        mesh.setPen(CosmeticMesh.SOLID);
        buildHoop(mesh, 8.0f, 0.45f, 32, core);

        // Floating diamond star motes orbiting the perimeter
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6 - time * 0.6f;
            float r = 8.0f;
            float px = CosmeticGeometry.cos(a) * r;
            float pz = CosmeticGeometry.sin(a) * r;
            mesh.setPen(CosmeticMesh.BRIGHT);
            mesh.addGem(px, 0.2f, pz, 0.4f, core, glow);
            mesh.addHalo(px, 0.2f, pz, 1.8f, CosmeticMesh.applyAlpha(glow, 0.6f));
        }

        mesh.pop();
    }

    /**
     * Enhanced Vortex Embers: 32 sparks ascending with cyclonic vortex rotation.
     */
    private static void buildEmbers(CosmeticMesh mesh, int glow, int core, float time) {
        for (int i = 0; i < 32; i++) {
            float seed = i * 11.19f;
            float speed = 0.55f + (seed % 0.7f);
            float progress = (time * speed + seed) % 1.0f;
            float life = 1.0f - progress;

            // Cyclonic swirl: tighter at base, flares wider as it ascends
            float swirlAngle = time * 2.0f + seed * 3.8f + progress * 3.2f;
            float radius = 3.5f + progress * 6.5f + CosmeticGeometry.sin(seed * 2.5f) * 2.0f;
            float x = CosmeticGeometry.cos(swirlAngle) * radius;
            float z = CosmeticGeometry.sin(swirlAngle) * radius;
            float y = -2.0f + progress * 26.0f;

            float size = 0.32f + life * 0.35f;
            int c = CosmeticMesh.blendColor(glow, core, progress);

            mesh.setPen(CosmeticMesh.BRIGHT);
            mesh.addBox(x - size, y - size, z - size, x + size, y + size, z + size, c);
            mesh.addHalo(x, y, z, size * 5.2f, CosmeticMesh.applyAlpha(glow, life * 0.65f));
        }
    }

    /**
     * Enhanced Gyroscopic Planetary Rings: 3 precession rings with varying inclinations and contra-rotation.
     */
    private static void buildRings(CosmeticMesh mesh, int glow, int core, float time) {
        float[] inclinations = {0.62f, -0.48f, 0.25f};
        float[] speeds = {0.95f, -0.8f, 1.3f};
        float[] radii = {9.5f, 11.2f, 13.0f};

        for (int i = 0; i < 3; i++) {
            mesh.push();
            mesh.translate(0, 7.5f, 0);

            mesh.rotateY(time * speeds[i] + i * 1.4f);
            mesh.rotateX(inclinations[i]);
            mesh.rotateZ(0.22f * (i - 1));

            float radius = radii[i];
            mesh.setPen(CosmeticMesh.CLEAR);
            buildBand(mesh, 0, radius, 1.8f, 0.45f, 36, CosmeticMesh.applyAlpha(core, 0.8f), CosmeticMesh.applyAlpha(glow, 0.0f), 0);
            mesh.setPen(CosmeticMesh.SOLID);
            buildHoop(mesh, radius, 0.4f, 36, core);

            // Add an orbiting pearl on each ring
            double pearlAngle = time * 2.0f * (i % 2 == 0 ? 1 : -1) + i * 2.0;
            float px = CosmeticGeometry.cos(pearlAngle) * radius;
            float pz = CosmeticGeometry.sin(pearlAngle) * radius;
            mesh.setPen(CosmeticMesh.BRIGHT);
            mesh.addGem(px, 0, pz, 0.5f, core, glow);
            mesh.addHalo(px, 0, pz, 2.2f, CosmeticMesh.applyAlpha(glow, 0.7f));

            mesh.pop();
        }
    }

    /**
     * Enhanced Triple-Helix Spiral: 3 ascending harmonic arms with interlocking pulse motes.
     */
    private static void buildSpiral(CosmeticMesh mesh, int glow, int core, float time) {
        for (int arm = 0; arm < 3; arm++) {
            float armOffset = arm * (float) (Math.PI * 2.0 / 3.0);
            for (int i = 0; i < 30; i++) {
                float frac = (float) i / 30.0f;
                float angle = time * 2.2f + armOffset + frac * (float) Math.PI * 3.4f;
                float radius = 9.5f * (1.0f - frac * 0.35f);
                float x = CosmeticGeometry.cos(angle) * radius;
                float z = CosmeticGeometry.sin(angle) * radius;
                float y = frac * 24.0f - 3.0f;

                float nodeSize = 0.38f + (1.0f - frac) * 0.28f;
                int c = CosmeticMesh.blendColor(core, glow, frac);

                mesh.setPen(CosmeticMesh.BRIGHT);
                mesh.addBox(x - nodeSize, y - nodeSize, z - nodeSize, x + nodeSize, y + nodeSize, z + nodeSize, c);
                mesh.addHalo(x, y, z, nodeSize * 4.8f, CosmeticMesh.applyAlpha(glow, (1.0f - frac) * 0.6f));
            }
        }
    }

    /**
     * Enhanced Prismatic Drift: Octahedral gem shards floating with harmonic breathing.
     */
    private static void buildDrift(CosmeticMesh mesh, int base, int glow, int core, float time, boolean spectralMode) {
        for (int i = 0; i < 18; i++) {
            float phase = time * 0.85f + i * 0.55f;
            float r = 9.0f + CosmeticGeometry.sin(phase * 1.3f) * 2.8f;
            double a = phase + (i * Math.PI * 2 / 18);

            float x = CosmeticGeometry.cos(a) * r;
            float z = CosmeticGeometry.sin(a) * r;
            float y = 5.5f + CosmeticGeometry.sin(phase * 1.8f + i) * 5.5f;

            int col = spectralMode
                ? CosmeticMesh.blendColor(glow, core, (CosmeticGeometry.sin(phase * 1.2f) + 1.0f) * 0.5f)
                : CosmeticMesh.blendColor(base == 0 ? glow : base, core, (float) i / 18.0f);

            mesh.setPen(CosmeticMesh.BRIGHT);
            mesh.addGem(x, y, z, 0.85f, col, CosmeticMesh.shadeColor(col, 0.82f));
            mesh.addHalo(x, y, z, 2.8f, CosmeticMesh.applyAlpha(col, 0.55f));
        }
    }
}
