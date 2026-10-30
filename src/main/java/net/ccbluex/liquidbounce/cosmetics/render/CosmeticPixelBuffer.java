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

public final class CosmeticPixelBuffer {

    public final int width;
    public final int height;
    public final byte[] pixels;

    public CosmeticPixelBuffer(int width, int height) {
        this.width = width;
        this.height = height;
        this.pixels = new byte[width * height];
    }

    public int get(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return 0;
        }
        return pixels[y * width + x];
    }

    public void set(int x, int y, int value) {
        if (x >= 0 && y >= 0 && x < width && y < height) {
            pixels[y * width + x] = (byte) value;
        }
    }

    @FunctionalInterface
    public interface Shader {
        int sample(float progressAlong, float progressAcross);
    }

    public void drawCapsule(
        float x0, float y0,
        float x1, float y1,
        float r0, float r1,
        Shader shader
    ) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float lenSq = dx * dx + dy * dy;

        int minX = (int) Math.floor(Math.min(x0, x1) - Math.max(r0, r1));
        int maxX = (int) Math.ceil(Math.max(x0, x1) + Math.max(r0, r1));
        int minY = (int) Math.floor(Math.min(y0, y1) - Math.max(r0, r1));
        int maxY = (int) Math.ceil(Math.max(y0, y1) + Math.max(r0, r1));

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                float px = x + 0.5f;
                float py = y + 0.5f;
                float t = (lenSq == 0) ? 0 : Math.max(0.0f, Math.min(1.0f, ((px - x0) * dx + (py - y0) * dy) / lenSq));
                float cx = x0 + dx * t;
                float cy = y0 + dy * t;
                float dist = (float) Math.hypot(px - cx, py - cy);
                float radius = r0 + (r1 - r0) * t;

                if (dist <= radius) {
                    int sample = shader.sample(t, radius <= 0 ? 0 : dist / radius);
                    if (sample != 0) {
                        set(x, y, sample);
                    }
                }
            }
        }
    }

    public void drawLine(float x0, float y0, float x1, float y1, float radius, int value) {
        drawCapsule(x0, y0, x1, y1, radius, radius, (along, across) -> value);
    }

    public void drawTriangle(
        float ax, float ay,
        float bx, float by,
        float cx, float cy,
        Shader shader
    ) {
        int minX = (int) Math.floor(Math.min(ax, Math.min(bx, cx)));
        int maxX = (int) Math.ceil(Math.max(ax, Math.max(bx, cx)));
        int minY = (int) Math.floor(Math.min(ay, Math.min(by, cy)));
        int maxY = (int) Math.ceil(Math.max(ay, Math.max(by, cy)));

        float area = (bx - ax) * (cy - ay) - (cx - ax) * (by - ay);
        if (Math.abs(area) < 1e-4f) {
            return;
        }

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                float px = x + 0.5f;
                float py = y + 0.5f;
                float u = ((bx - px) * (cy - py) - (cx - px) * (by - py)) / area;
                float v = ((cx - px) * (ay - py) - (ax - px) * (cy - py)) / area;
                float k = 1.0f - u - v;

                if (u < 0 || v < 0 || k < 0) {
                    continue;
                }

                int color = shader.sample(u, k);
                if (color != 0) {
                    set(x, y, color);
                }
            }
        }
    }

    public void drawEllipse(float cx, float cy, float rx, float ry, float angleRad, Shader shader) {
        float cos = (float) Math.cos(angleRad);
        float sin = (float) Math.sin(angleRad);
        int bounds = (int) Math.ceil(Math.max(rx, ry)) + 1;

        for (int y = (int) cy - bounds; y <= (int) cy + bounds; y++) {
            for (int x = (int) cx - bounds; x <= (int) cx + bounds; x++) {
                float px = x + 0.5f - cx;
                float py = y + 0.5f - cy;
                float u = (px * cos + py * sin) / rx;
                float v = (-px * sin + py * cos) / ry;
                float distSq = u * u + v * v;

                if (distSq <= 1.0f) {
                    int sample = shader.sample((float) Math.sqrt(distSq), (float) Math.atan2(v, u));
                    if (sample != 0) {
                        set(x, y, sample);
                    }
                }
            }
        }
    }

    public void traceOutline(int fillValue, int edgeValue) {
        byte[] snapshot = pixels.clone();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (snapshot[y * width + x] != fillValue) {
                    continue;
                }

                boolean isExposed = (x == 0 || y == 0 || x == width - 1 || y == height - 1)
                    || snapshot[y * width + x - 1] == 0
                    || snapshot[y * width + x + 1] == 0
                    || snapshot[(y - 1) * width + x] == 0
                    || snapshot[(y + 1) * width + x] == 0;

                if (isExposed) {
                    pixels[y * width + x] = (byte) edgeValue;
                }
            }
        }
    }

    public void rasterizeToMesh(
        CosmeticMesh mesh,
        int[] palette,
        byte[] penTypes,
        float cellSize,
        float depth,
        int startX,
        int endX,
        float originX,
        float originY,
        int sideSign
    ) {
        float z0 = -depth / 2.0f;
        float z1 = depth / 2.0f;

        for (int y = 0; y < height; y++) {
            int x = startX;
            while (x < endX) {
                int val = get(x, y);
                if (val == 0) {
                    x++;
                    continue;
                }
                int runEnd = x + 1;
                while (runEnd < endX && get(runEnd, y) == val) {
                    runEnd++;
                }

                float xa = originX + x * cellSize;
                float xb = originX + runEnd * cellSize;
                float ya = originY + y * cellSize;
                float yb = ya + cellSize;

                mesh.setPen(penTypes[val]);
                emitSideFace(mesh, sideSign, xa, ya, z0, xa, yb, z0, xb, yb, z0, xb, ya, z0, palette[val]);
                emitSideFace(mesh, sideSign, xb, ya, z1, xb, yb, z1, xa, yb, z1, xa, ya, z1, palette[val]);
                x = runEnd;
            }
        }

        for (int y = 0; y <= height; y++) {
            int x = startX;
            while (x < endX) {
                int boundary = computeVerticalEdge(x, y);
                if (boundary == 0) {
                    x++;
                    continue;
                }
                int runEnd = x + 1;
                while (runEnd < endX && computeVerticalEdge(runEnd, y) == boundary) {
                    runEnd++;
                }

                float xa = originX + x * cellSize;
                float xb = originX + runEnd * cellSize;
                float edgeY = originY + y * cellSize;
                int shaded = CosmeticMesh.shadeColor(palette[Math.abs(boundary)], 0.82f);

                mesh.setPen(penTypes[Math.abs(boundary)]);
                if (boundary > 0) {
                    emitSideFace(mesh, sideSign, xa, edgeY, z1, xb, edgeY, z1, xb, edgeY, z0, xa, edgeY, z0, shaded);
                } else {
                    emitSideFace(mesh, sideSign, xa, edgeY, z0, xb, edgeY, z0, xb, edgeY, z1, xa, edgeY, z1, shaded);
                }
                x = runEnd;
            }
        }

        for (int x = startX; x <= endX; x++) {
            int y = 0;
            while (y < height) {
                int boundary = computeHorizontalEdge(x, y, startX, endX);
                if (boundary == 0) {
                    y++;
                    continue;
                }
                int runEnd = y + 1;
                while (runEnd < height && computeHorizontalEdge(x, runEnd, startX, endX) == boundary) {
                    runEnd++;
                }

                float edgeX = originX + x * cellSize;
                float ya = originY + y * cellSize;
                float yb = originY + runEnd * cellSize;
                int shaded = CosmeticMesh.shadeColor(palette[Math.abs(boundary)], 0.82f);

                mesh.setPen(penTypes[Math.abs(boundary)]);
                if (boundary > 0) {
                    emitSideFace(mesh, sideSign, edgeX, ya, z0, edgeX, yb, z0, edgeX, yb, z1, edgeX, ya, z1, shaded);
                } else {
                    emitSideFace(mesh, sideSign, edgeX, ya, z1, edgeX, yb, z1, edgeX, yb, z0, edgeX, ya, z0, shaded);
                }
                y = runEnd;
            }
        }

        mesh.setPen(CosmeticMesh.SOLID);
    }

    private int computeVerticalEdge(int x, int y) {
        int above = get(x, y - 1);
        int below = get(x, y);
        if (above != 0 && below == 0) {
            return above;
        }
        if (above == 0 && below != 0) {
            return -below;
        }
        return 0;
    }

    private int computeHorizontalEdge(int x, int y, int minX, int maxX) {
        int left = (x > minX) ? get(x - 1, y) : 0;
        int right = (x < maxX) ? get(x, y) : 0;
        if (left != 0 && right == 0) {
            return left;
        }
        if (left == 0 && right != 0) {
            return -right;
        }
        return 0;
    }

    private static void emitSideFace(
        CosmeticMesh mesh,
        int sideSign,
        float x0, float y0, float z0,
        float x1, float y1, float z1,
        float x2, float y2, float z2,
        float x3, float y3, float z3,
        int color
    ) {
        if (sideSign > 0) {
            mesh.addQuad(x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, color);
        } else {
            mesh.addQuad(-x3, y3, z3, -x2, y2, z2, -x1, y1, z1, -x0, y0, z0, color);
        }
    }
}
