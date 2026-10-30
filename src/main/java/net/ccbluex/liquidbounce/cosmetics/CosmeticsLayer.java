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

package net.ccbluex.liquidbounce.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ccbluex.liquidbounce.cosmetics.data.PlayerCosmeticTracker;
import net.ccbluex.liquidbounce.cosmetics.data.PlayerWardrobe;
import net.ccbluex.liquidbounce.cosmetics.render.CosmeticMesh;
import net.ccbluex.liquidbounce.cosmetics.render.CosmeticRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Custom player cosmetic render layer (auras, wings, hats, accessories).
 */
public final class CosmeticsLayer extends RenderLayer<AvatarRenderState, PlayerModel> {

    private static final Identifier WHITE = Identifier.fromNamespaceAndPath("liquidbounce", "textures/white.png");
    private static final Identifier GLOW = Identifier.fromNamespaceAndPath("liquidbounce", "textures/glow.png");

    private static final RenderType SOLID = RenderTypes.entityCutout(WHITE);
    private static final RenderType CLEAR = RenderTypes.entityTranslucent(WHITE);
    private static final RenderType GLOW_TYPE = RenderTypes.entityTranslucentEmissive(WHITE);
    private static final RenderType HALO = RenderTypes.eyes(GLOW);

    public CosmeticsLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
        super(parent);
    }

    @Override
    public void submit(
        PoseStack pose,
        SubmitNodeCollector collector,
        int light,
        AvatarRenderState state,
        float yRot,
        float xRot
    ) {
        if (state.isInvisible) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.level.getEntity(state.id) instanceof AbstractClientPlayer player)) {
            return;
        }

        boolean isSelf = (player == mc.player) || (mc.getUser() != null && player.getUUID().equals(mc.getUser().getProfileId()));
        PlayerCosmeticTracker.PlayerOutfit worn = isSelf ? PlayerWardrobe.getWornOutfit() : PlayerCosmeticTracker.query(player.getUUID(), false);
        if (worn == null || worn.isEmpty()) {
            return;
        }

        UUID id = player.getUUID();
        float time = CosmeticRenderer.computeAnimationTime(id);
        float air = (player.isFallFlying() || player.getAbilities().flying) ? 1.0f : (player.onGround() ? 0.0f : 0.3f);

        // Render Aura
        if (!worn.aura().isEmpty()) {
            submitGeometry(pose, collector, light, () -> CosmeticRenderer.renderAura(id, worn.aura(), time));
        }

        // Render Wings
        if (!worn.wings().isEmpty() && !player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
            pose.pushPose();
            getParentModel().body.translateAndRotate(pose);
            submitGeometry(pose, collector, light, () -> CosmeticRenderer.renderWings(id, worn.wings(), time, player.getX(), player.getZ(), air));
            pose.popPose();
        }

        // Render Hat
        if (!worn.headwear().isEmpty()) {
            pose.pushPose();
            getParentModel().head.translateAndRotate(pose);
            if (!player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                pose.translate(0, -0.26f, 0);
                pose.scale(1.08f, 1.08f, 1.08f);
                pose.translate(0, 0.25f, 0);
            }
            submitGeometry(pose, collector, light, () -> CosmeticRenderer.renderHeadwear(id, worn.headwear(), time));
            pose.popPose();
        }
    }

    private static void submitGeometry(
        PoseStack pose,
        SubmitNodeCollector collector,
        int light,
        Supplier<CosmeticMesh> meshSupplier
    ) {
        final CosmeticMesh mesh = meshSupplier.get();
        if (mesh == null) {
            return;
        }
        if (CosmeticRenderer.containsGeometry(mesh, CosmeticMesh.SOLID)) {
            collector.submitCustomGeometry(
                pose,
                SOLID,
                (p, c) -> CosmeticRenderer.submitMesh(mesh, CosmeticMesh.SOLID, p.pose(), p.normal(), light, OverlayTexture.NO_OVERLAY, c::addVertex)
            );
        }
        if (CosmeticRenderer.containsGeometry(mesh, CosmeticMesh.CLEAR)) {
            collector.submitCustomGeometry(
                pose,
                CLEAR,
                (p, c) -> CosmeticRenderer.submitMesh(mesh, CosmeticMesh.CLEAR, p.pose(), p.normal(), light, OverlayTexture.NO_OVERLAY, c::addVertex)
            );
        }
        if (CosmeticRenderer.containsGeometry(mesh, CosmeticMesh.GLOW)) {
            collector.submitCustomGeometry(
                pose,
                GLOW_TYPE,
                (p, c) -> CosmeticRenderer.submitMesh(mesh, CosmeticMesh.GLOW, p.pose(), p.normal(), light, OverlayTexture.NO_OVERLAY, c::addVertex)
            );
        }
        if (CosmeticRenderer.containsGeometry(mesh, CosmeticMesh.HALO)) {
            collector.submitCustomGeometry(
                pose,
                HALO,
                (p, c) -> CosmeticRenderer.submitHalos(mesh, p.pose(), OverlayTexture.NO_OVERLAY, c::addVertex)
            );
        }
    }
}
