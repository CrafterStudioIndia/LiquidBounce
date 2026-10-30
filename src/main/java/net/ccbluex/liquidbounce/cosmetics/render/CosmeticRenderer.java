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

import net.ccbluex.liquidbounce.cosmetics.data.OutfitConfig;
import net.ccbluex.liquidbounce.cosmetics.features.AuraEffect;
import net.ccbluex.liquidbounce.cosmetics.features.HeadwearCosmetic;
import net.ccbluex.liquidbounce.cosmetics.features.WingCosmetic;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CosmeticRenderer {

    @FunctionalInterface
    public interface VertexTarget {
        void emit(
            float x, float y, float z,
            int argb,
            float u, float v,
            int overlay,
            int light,
            float nx, float ny, float nz
        );
    }

    private static final Vector3f POS = new Vector3f();
    private static final Vector3f NORM = new Vector3f();
    private static final Vector3f RIGHT = new Vector3f();
    private static final Vector3f UP = new Vector3f();

    private static final int FULL_LIGHT = 0xF000F0;
    private static final long CACHE_TTL_NANOS = 100_000_000L;
    private static final Map<UUID, EntityMovementProfile> TRACKED_ENTITIES = new HashMap<>();
    private static long lastCleanupNanos;

    private static final class MeshCacheNode {
        final CosmeticMesh mesh = new CosmeticMesh();
        OutfitConfig config;
        long generatedAt;

        boolean isValid(OutfitConfig current, long now) {
            return current.equals(config) && (now - generatedAt < CACHE_TTL_NANOS);
        }

        CosmeticMesh commit(OutfitConfig current, long now) {
            this.config = current;
            this.generatedAt = now;
            return mesh;
        }
    }

    private static final class EntityMovementProfile {
        final MeshCacheNode headwearCache = new MeshCacheNode();
        final MeshCacheNode wingCache = new MeshCacheNode();
        final MeshCacheNode auraCache = new MeshCacheNode();

        long lastSeen;
        long lastMovementTick;
        double posX;
        double posZ;
        float currentSpeed;
        float animatedPace;
        float animationPhase;
        boolean hasInitialized;
    }

    private CosmeticRenderer() {}

    public static float computeAnimationTime(UUID entityId) {
        return (System.nanoTime() % 1_000_000_000_000L) / 1e9f + (entityId.hashCode() & 1023) * 0.37f;
    }

    private static EntityMovementProfile obtainProfile(UUID id, long now) {
        if (now - lastCleanupNanos > 5_000_000_000L) {
            lastCleanupNanos = now;
            TRACKED_ENTITIES.values().removeIf(profile -> now - profile.lastSeen > 10_000_000_000L);
        }

        EntityMovementProfile profile = TRACKED_ENTITIES.computeIfAbsent(id, k -> new EntityMovementProfile());
        profile.lastSeen = now;
        return profile;
    }

    public static CosmeticMesh renderHeadwear(UUID id, OutfitConfig outfit, float timeSeconds) {
        long now = System.nanoTime();
        MeshCacheNode cache = obtainProfile(id, now).headwearCache;
        if (cache.isValid(outfit, now)) {
            return cache.mesh;
        }
        cache.mesh.reset();
        HeadwearCosmetic.generateMesh(cache.mesh, outfit, timeSeconds);
        return cache.commit(outfit, now);
    }

    public static CosmeticMesh renderAura(UUID id, OutfitConfig outfit, float timeSeconds) {
        long now = System.nanoTime();
        MeshCacheNode cache = obtainProfile(id, now).auraCache;
        if (cache.isValid(outfit, now)) {
            return cache.mesh;
        }
        cache.mesh.reset();
        AuraEffect.generateMesh(cache.mesh, outfit, timeSeconds);
        return cache.commit(outfit, now);
    }

    public static CosmeticMesh renderWings(UUID id, OutfitConfig outfit, float timeSeconds, double x, double z, float airFactor) {
        long now = System.nanoTime();
        EntityMovementProfile profile = obtainProfile(id, now);
        updateAnimationState(profile, now, x, z, airFactor);

        if (profile.wingCache.isValid(outfit, now)) {
            return profile.wingCache.mesh;
        }
        profile.wingCache.mesh.reset();
        WingCosmetic.generateMesh(profile.wingCache.mesh, outfit, timeSeconds, profile.animationPhase, profile.animatedPace);
        return profile.wingCache.commit(outfit, now);
    }

    private static void updateAnimationState(EntityMovementProfile profile, long now, double x, double z, float airFactor) {
        if (!profile.hasInitialized) {
            profile.hasInitialized = true;
            profile.lastMovementTick = now;
            profile.posX = x;
            profile.posZ = z;
            return;
        }

        float dt = Math.min(0.1f, (now - profile.lastMovementTick) / 1e9f);
        if (dt <= 0) {
            return;
        }

        profile.lastMovementTick = now;
        float distanceStep = (float) Math.min(80, Math.hypot(x - profile.posX, z - profile.posZ) / dt);
        profile.posX = x;
        profile.posZ = z;
        profile.currentSpeed += (distanceStep - profile.currentSpeed) * (1.0f - (float) Math.exp(-dt / 0.3f));

        float targetPace = Math.min(1.0f, Math.min(0.6f, profile.currentSpeed / 8.0f) + airFactor);
        profile.animatedPace += (targetPace - profile.animatedPace) * (1.0f - (float) Math.exp(-dt / 0.35f));
        profile.animationPhase = (profile.animationPhase + dt * (1.6f + 5.0f * profile.animatedPace)) % 6283.1853f;
    }

    private static boolean matchQuadType(byte quadType, byte targetKind) {
        return (quadType == targetKind) || (targetKind == CosmeticMesh.SOLID && quadType == CosmeticMesh.BRIGHT);
    }

    public static boolean containsGeometry(CosmeticMesh mesh, byte kind) {
        byte[] kinds = mesh.getKinds();
        for (int i = 0; i < mesh.getQuadCount(); i++) {
            if (matchQuadType(kinds[i], kind)) {
                return true;
            }
        }
        return false;
    }

    public static void submitMesh(
        CosmeticMesh mesh,
        byte targetKind,
        Matrix4f poseMatrix,
        Matrix3f normalMatrix,
        int packedLight,
        int packedOverlay,
        VertexTarget target
    ) {
        float[] positions = mesh.getPositions();
        float[] normals = mesh.getNormals();
        int[] colors = mesh.getColors();
        byte[] kinds = mesh.getKinds();

        for (int q = 0; q < mesh.getQuadCount(); q++) {
            if (!matchQuadType(kinds[q], targetKind)) {
                continue;
            }

            int lighting = (kinds[q] == CosmeticMesh.GLOW || kinds[q] == CosmeticMesh.BRIGHT) ? FULL_LIGHT : packedLight;
            if (kinds[q] == CosmeticMesh.BRIGHT) {
                normalMatrix.transform(NORM.set(0, -1, 0));
            } else {
                normalMatrix.transform(NORM.set(normals[q * 3], normals[q * 3 + 1], normals[q * 3 + 2]));
            }

            for (int v = 0; v < 4; v++) {
                int at = q * 12 + v * 3;
                poseMatrix.transformPosition(POS.set(positions[at] / 16.0f, positions[at + 1] / 16.0f, positions[at + 2] / 16.0f));
                target.emit(POS.x, POS.y, POS.z, colors[q * 4 + v], 0.5f, 0.5f, packedOverlay, lighting, NORM.x, NORM.y, NORM.z);
            }

            if (kinds[q] != CosmeticMesh.GLOW) {
                continue;
            }

            for (int v = 3; v >= 0; v--) {
                int at = q * 12 + v * 3;
                poseMatrix.transformPosition(POS.set(positions[at] / 16.0f, positions[at + 1] / 16.0f, positions[at + 2] / 16.0f));
                target.emit(POS.x, POS.y, POS.z, colors[q * 4 + v], 0.5f, 0.5f, packedOverlay, lighting, -NORM.x, -NORM.y, -NORM.z);
            }
        }
    }

    public static void submitHalos(
        CosmeticMesh mesh,
        Matrix4f poseMatrix,
        int packedOverlay,
        VertexTarget target
    ) {
        float[] positions = mesh.getPositions();
        float[] normals = mesh.getNormals();
        int[] colors = mesh.getColors();
        byte[] kinds = mesh.getKinds();

        float uniformScale = poseMatrix.transformDirection(NORM.set(1, 0, 0)).length() / 16.0f;

        for (int q = 0; q < mesh.getQuadCount(); q++) {
            if (kinds[q] != CosmeticMesh.HALO) {
                continue;
            }

            poseMatrix.transformPosition(POS.set(positions[q * 12] / 16.0f, positions[q * 12 + 1] / 16.0f, positions[q * 12 + 2] / 16.0f));
            float radius = normals[q * 3] * uniformScale;

            if (POS.lengthSquared() < 1e-8f) {
                NORM.set(0, 0, 1);
            } else {
                POS.normalize(NORM);
            }

            NORM.cross(0, 1, 0, RIGHT);
            if (RIGHT.lengthSquared() < 1e-6f) {
                RIGHT.set(1, 0, 0);
            }
            RIGHT.normalize(radius);
            RIGHT.cross(NORM, UP).normalize(radius);

            int color = colors[q * 4];
            int alpha = color >>> 24;
            int litColor = 0xFF000000
                | ((color >> 16 & 255) * alpha / 255) << 16
                | ((color >> 8 & 255) * alpha / 255) << 8
                | (color & 255) * alpha / 255;

            target.emit(POS.x - RIGHT.x - UP.x, POS.y - RIGHT.y - UP.y, POS.z - RIGHT.z - UP.z, litColor, 0, 1, packedOverlay, FULL_LIGHT, 0, 1, 0);
            target.emit(POS.x + RIGHT.x - UP.x, POS.y + RIGHT.y - UP.y, POS.z + RIGHT.z - UP.z, litColor, 1, 1, packedOverlay, FULL_LIGHT, 0, 1, 0);
            target.emit(POS.x + RIGHT.x + UP.x, POS.y + RIGHT.y + UP.y, POS.z + RIGHT.z + UP.z, litColor, 1, 0, packedOverlay, FULL_LIGHT, 0, 1, 0);
            target.emit(POS.x - RIGHT.x + UP.x, POS.y - RIGHT.y + UP.y, POS.z - RIGHT.z + UP.z, litColor, 0, 0, packedOverlay, FULL_LIGHT, 0, 1, 0);
        }
    }
}

