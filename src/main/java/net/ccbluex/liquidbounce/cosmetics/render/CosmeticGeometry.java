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

package net.ccbluex.liquidbounce.cosmetics.render;

public final class CosmeticGeometry {

    private CosmeticGeometry() {}

    public static float cos(double angle) {
        return (float) Math.cos(angle);
    }

    public static float sin(double angle) {
        return (float) Math.sin(angle);
    }

    public static float squircle(double angle, float exponent) {
        if (exponent <= 2.01f) {
            return 1.0f;
        }
        double c = Math.abs(Math.cos(angle));
        double s = Math.abs(Math.sin(angle));
        return (float) (1.0 / Math.pow(Math.pow(c, exponent) + Math.pow(s, exponent), 1.0 / exponent));
    }

    public static void buildLoft(
        CosmeticMesh mesh,
        float[] yLevels,
        float[] radii,
        float[] xOffsets,
        float[] zOffsets,
        int[] levelColors,
        int segmentCount,
        float exponent,
        float stripeIntensity
    ) {
        for (int level = 0; level + 1 < yLevels.length; level++) {
            for (int seg = 0; seg < segmentCount; seg++) {
                double a = Math.PI * 2 * seg / segmentCount;
                double b = Math.PI * 2 * (seg + 1) / segmentCount;

                float ka = squircle(a, exponent);
                float kb = squircle(b, exponent);

                float ax0 = cos(a) * ka * radii[level] + xOffsets[level];
                float az0 = sin(a) * ka * radii[level] + zOffsets[level];
                float bx0 = cos(b) * kb * radii[level] + xOffsets[level];
                float bz0 = sin(b) * kb * radii[level] + zOffsets[level];

                float ax1 = cos(a) * ka * radii[level + 1] + xOffsets[level + 1];
                float az1 = sin(a) * ka * radii[level + 1] + zOffsets[level + 1];
                float bx1 = cos(b) * kb * radii[level + 1] + xOffsets[level + 1];
                float bz1 = sin(b) * kb * radii[level + 1] + zOffsets[level + 1];

                float stripeFactor = (stripeIntensity > 0 && (seg & 1) == 0) ? (1.0f - stripeIntensity) : 1.0f;
                int c0 = CosmeticMesh.shadeColor(levelColors[level], stripeFactor);
                int c1 = CosmeticMesh.shadeColor(levelColors[level + 1], stripeFactor);

                mesh.addQuad(
                    ax0, yLevels[level], az0,
                    ax1, yLevels[level + 1], az1,
                    bx1, yLevels[level + 1], bz1,
                    bx0, yLevels[level], bz0,
                    c0, c1, c1, c0
                );
            }
        }
    }

    public static void buildSphere(
        CosmeticMesh mesh,
        float cx, float cy, float cz,
        float radius,
        int color,
        int ringCount,
        int segmentCount
    ) {
        for (int ring = 0; ring < ringCount; ring++) {
            double p0 = Math.PI * ring / ringCount - Math.PI / 2;
            double p1 = Math.PI * (ring + 1) / ringCount - Math.PI / 2;

            float y0 = sin(p0) * radius;
            float y1 = sin(p1) * radius;
            float r0 = cos(p0) * radius;
            float r1 = cos(p1) * radius;

            for (int seg = 0; seg < segmentCount; seg++) {
                double a = Math.PI * 2 * seg / segmentCount;
                double b = Math.PI * 2 * (seg + 1) / segmentCount;

                mesh.addQuad(
                    cx + cos(a) * r0, cy + y0, cz + sin(a) * r0,
                    cx + cos(a) * r1, cy + y1, cz + sin(a) * r1,
                    cx + cos(b) * r1, cy + y1, cz + sin(b) * r1,
                    cx + cos(b) * r0, cy + y0, cz + sin(b) * r0,
                    color
                );
            }
        }
    }
}

