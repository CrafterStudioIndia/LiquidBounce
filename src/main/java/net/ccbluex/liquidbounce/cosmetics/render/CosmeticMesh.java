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

import java.util.Arrays;

/**
 * High-performance vertex and polygon builder for custom client-side cosmetics.
 */
public final class CosmeticMesh {

    public static final byte SOLID = 0;
    public static final byte CLEAR = 1;
    public static final byte GLOW = 2;
    public static final byte BRIGHT = 3;
    public static final byte HALO = 4;

    private float[] positions = new float[12 * 256];
    private float[] normals = new float[3 * 256];
    private int[] colors = new int[4 * 256];
    private byte[] kinds = new byte[256];
    private int quadCount;

    private final float[][] matrixStack = new float[16][16];
    private int stackDepth;
    private float[] currentMatrix = matrixStack[0];
    private byte currentPen = SOLID;

    public CosmeticMesh() {
        setIdentity(currentMatrix);
    }

    private static void setIdentity(float[] matrix) {
        Arrays.fill(matrix, 0);
        matrix[0] = matrix[5] = matrix[10] = matrix[15] = 1.0f;
    }

    public void reset() {
        quadCount = 0;
        stackDepth = 0;
        currentMatrix = matrixStack[0];
        setIdentity(currentMatrix);
        currentPen = SOLID;
    }

    public int getQuadCount() {
        return quadCount;
    }

    public float[] getPositions() {
        return positions;
    }

    public float[] getNormals() {
        return normals;
    }

    public int[] getColors() {
        return colors;
    }

    public byte[] getKinds() {
        return kinds;
    }

    public CosmeticMesh setPen(byte pen) {
        this.currentPen = pen;
        return this;
    }

    public byte getPen() {
        return currentPen;
    }

    public void push() {
        float[] next = matrixStack[++stackDepth];
        System.arraycopy(currentMatrix, 0, next, 0, 16);
        currentMatrix = next;
    }

    public void pop() {
        currentMatrix = matrixStack[--stackDepth];
    }

    private void multiply(
        float a0, float a1, float a2,
        float a4, float a5, float a6,
        float a8, float a9, float a10,
        float tx, float ty, float tz
    ) {
        float m0 = currentMatrix[0], m1 = currentMatrix[1], m2 = currentMatrix[2];
        float m4 = currentMatrix[4], m5 = currentMatrix[5], m6 = currentMatrix[6];
        float m8 = currentMatrix[8], m9 = currentMatrix[9], m10 = currentMatrix[10];

        currentMatrix[12] += m0 * tx + m4 * ty + m8 * tz;
        currentMatrix[13] += m1 * tx + m5 * ty + m9 * tz;
        currentMatrix[14] += m2 * tx + m6 * ty + m10 * tz;

        currentMatrix[0] = m0 * a0 + m4 * a1 + m8 * a2;
        currentMatrix[1] = m1 * a0 + m5 * a1 + m9 * a2;
        currentMatrix[2] = m2 * a0 + m6 * a1 + m10 * a2;

        currentMatrix[4] = m0 * a4 + m4 * a5 + m8 * a6;
        currentMatrix[5] = m1 * a4 + m5 * a5 + m9 * a6;
        currentMatrix[6] = m2 * a4 + m6 * a5 + m10 * a6;

        currentMatrix[8] = m0 * a8 + m4 * a9 + m8 * a10;
        currentMatrix[9] = m1 * a8 + m5 * a9 + m9 * a10;
        currentMatrix[10] = m2 * a8 + m6 * a9 + m10 * a10;
    }

    public void translate(float x, float y, float z) {
        multiply(1, 0, 0, 0, 1, 0, 0, 0, 1, x, y, z);
    }

    public void scale(float x, float y, float z) {
        multiply(x, 0, 0, 0, y, 0, 0, 0, z, 0, 0, 0);
    }

    public void rotateX(float angleRad) {
        float c = (float) Math.cos(angleRad);
        float s = (float) Math.sin(angleRad);
        multiply(1, 0, 0, 0, c, s, 0, -s, c, 0, 0, 0);
    }

    public void rotateY(float angleRad) {
        float c = (float) Math.cos(angleRad);
        float s = (float) Math.sin(angleRad);
        multiply(c, 0, -s, 0, 1, 0, s, 0, c, 0, 0, 0);
    }

    public void rotateZ(float angleRad) {
        float c = (float) Math.cos(angleRad);
        float s = (float) Math.sin(angleRad);
        multiply(c, s, 0, -s, c, 0, 0, 0, 1, 0, 0, 0);
    }

    private void expandBuffers() {
        int nextCapacity = kinds.length * 2;
        positions = Arrays.copyOf(positions, nextCapacity * 12);
        normals = Arrays.copyOf(normals, nextCapacity * 3);
        colors = Arrays.copyOf(colors, nextCapacity * 4);
        kinds = Arrays.copyOf(kinds, nextCapacity);
    }

    private void storeVertex(int offset, float x, float y, float z) {
        positions[offset] = currentMatrix[0] * x + currentMatrix[4] * y + currentMatrix[8] * z + currentMatrix[12];
        positions[offset + 1] = currentMatrix[1] * x + currentMatrix[5] * y + currentMatrix[9] * z + currentMatrix[13];
        positions[offset + 2] = currentMatrix[2] * x + currentMatrix[6] * y + currentMatrix[10] * z + currentMatrix[14];
    }

    public void addQuad(
        float x0, float y0, float z0,
        float x1, float y1, float z1,
        float x2, float y2, float z2,
        float x3, float y3, float z3,
        int c0, int c1, int c2, int c3
    ) {
        if (quadCount == kinds.length) {
            expandBuffers();
        }

        int p = quadCount * 12;
        storeVertex(p, x0, y0, z0);
        storeVertex(p + 3, x1, y1, z1);
        storeVertex(p + 6, x2, y2, z2);
        storeVertex(p + 9, x3, y3, z3);

        float ax = positions[p + 6] - positions[p];
        float ay = positions[p + 7] - positions[p + 1];
        float az = positions[p + 8] - positions[p + 2];

        float bx = positions[p + 9] - positions[p + 3];
        float by = positions[p + 10] - positions[p + 4];
        float bz = positions[p + 11] - positions[p + 5];

        float nx = ay * bz - az * by;
        float ny = az * bx - ax * bz;
        float nz = ax * by - ay * bx;

        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-6f) {
            len = 1.0f;
        }

        normals[quadCount * 3] = nx / len;
        normals[quadCount * 3 + 1] = ny / len;
        normals[quadCount * 3 + 2] = nz / len;

        colors[quadCount * 4] = c0;
        colors[quadCount * 4 + 1] = c1;
        colors[quadCount * 4 + 2] = c2;
        colors[quadCount * 4 + 3] = c3;

        kinds[quadCount] = currentPen;
        quadCount++;
    }

    public void addQuad(
        float x0, float y0, float z0,
        float x1, float y1, float z1,
        float x2, float y2, float z2,
        float x3, float y3, float z3,
        int c
    ) {
        addQuad(x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, c, c, c, c);
    }

    public void addTriangle(
        float x0, float y0, float z0,
        float x1, float y1, float z1,
        float x2, float y2, float z2,
        int c0, int c1, int c2
    ) {
        addQuad(x0, y0, z0, x1, y1, z1, x2, y2, z2, x2, y2, z2, c0, c1, c2, c2);
    }

    public void addHalo(float x, float y, float z, float radius, int color) {
        if ((color >>> 24) < 3) {
            return;
        }
        byte previousPen = currentPen;
        currentPen = HALO;
        addQuad(x, y, z, x, y, z, x, y, z, x, y, z, color);
        currentPen = previousPen;
        normals[(quadCount - 1) * 3] = radius * (float) Math.sqrt(
            currentMatrix[0] * currentMatrix[0] + currentMatrix[1] * currentMatrix[1] + currentMatrix[2] * currentMatrix[2]
        );
    }

    public void addBox(float x0, float y0, float z0, float x1, float y1, float z1, int c) {
        addBox(x0, y0, z0, x1, y1, z1, c, c, c);
    }

    public void addBox(float x0, float y0, float z0, float x1, float y1, float z1, int top, int side, int bottom) {
        addQuad(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, top);
        addQuad(x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, bottom);
        addQuad(x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, side);
        addQuad(x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1, side);
        addQuad(x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0, side);
        addQuad(x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, side);
    }

    public void addDisc(float y, float r, int segments, int c, boolean facingUp) {
        for (int i = 0; i < segments; i += 2) {
            double a = Math.PI * 2 * i / segments;
            double b = Math.PI * 2 * (i + 1) / segments;
            double d = Math.PI * 2 * (i + 2) / segments;

            float ax = (float) Math.cos(a) * r;
            float az = (float) Math.sin(a) * r;
            float bx = (float) Math.cos(b) * r;
            float bz = (float) Math.sin(b) * r;
            float dx = (float) Math.cos(d) * r;
            float dz = (float) Math.sin(d) * r;

            if (facingUp) {
                addQuad(0, y, 0, ax, y, az, bx, y, bz, dx, y, dz, c);
            } else {
                addQuad(0, y, 0, dx, y, dz, bx, y, bz, ax, y, az, c);
            }
        }
    }

    public void addGem(float x, float y, float z, float r, int c, int edgeColor) {
        float[][] points = {
            {r, 0, 0},
            {0, 0, r},
            {-r, 0, 0},
            {0, 0, -r}
        };

        for (int i = 0; i < 4; i++) {
            float[] a = points[i];
            float[] b = points[(i + 1) % 4];
            addTriangle(x + a[0], y, z + a[2], x, y - r * 1.3f, z, x + b[0], y, z + b[2], edgeColor, c, edgeColor);
            addTriangle(x + b[0], y, z + b[2], x, y + r * 1.3f, z, x + a[0], y, z + a[2], edgeColor, c, edgeColor);
        }
    }

    public static int blendColor(int colorA, int colorB, float ratio) {
        ratio = Math.max(0.0f, Math.min(1.0f, ratio));
        int aa = colorA >>> 24, ar = colorA >> 16 & 255, ag = colorA >> 8 & 255, ab = colorA & 255;
        int ba = colorB >>> 24, br = colorB >> 16 & 255, bg = colorB >> 8 & 255, bb = colorB & 255;
        return Math.round(aa + (ba - aa) * ratio) << 24
            | Math.round(ar + (br - ar) * ratio) << 16
            | Math.round(ag + (bg - ag) * ratio) << 8
            | Math.round(ab + (bb - ab) * ratio);
    }

    public static int shadeColor(int argb, float factor) {
        int r = Math.min(255, Math.round((argb >> 16 & 255) * factor));
        int g = Math.min(255, Math.round((argb >> 8 & 255) * factor));
        int b = Math.min(255, Math.round((argb & 255) * factor));
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    public static int applyAlpha(int argb, float alpha) {
        return Math.round(Math.max(0.0f, Math.min(1.0f, alpha)) * 255.0f) << 24 | (argb & 0x00FFFFFF);
    }

    public static int toOpaque(int rgb) {
        return 0xFF000000 | rgb;
    }
}

