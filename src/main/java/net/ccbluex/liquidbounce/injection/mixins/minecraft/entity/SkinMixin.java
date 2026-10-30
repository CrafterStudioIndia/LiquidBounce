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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.entity;

import net.ccbluex.liquidbounce.cosmetics.CosmeticsEngine;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleSkinChanger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * Intercepts native Minecraft player skin texture queries `getSkin()`.
 * Handles local client skin overrides via [CosmeticsEngine.Library.active] and
 * remote player skins via [CosmeticsEngine.Looks.of].
 */
@Mixin(AbstractClientPlayer.class)
public abstract class SkinMixin {

    @Inject(method = "getSkin", at = @At("TAIL"), cancellable = true)
    private void liquid_bounce$interceptGetSkin(CallbackInfoReturnable<PlayerSkin> cir) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        Minecraft client = Minecraft.getInstance();
        UUID uuid = player.getUUID();
        boolean isLocal = client.player != null && uuid.equals(client.player.getUUID());

        // 1. If ModuleSkinChanger is explicitly enabled for local player, apply it
        if (isLocal && ModuleSkinChanger.shouldApplyChanges()) {
            var customSupplier = ModuleSkinChanger.INSTANCE.getSkinTextures();
            if (customSupplier != null) {
                PlayerSkin original = cir.getReturnValue();
                PlayerSkin customTextures = customSupplier.get();
                cir.setReturnValue(new PlayerSkin(
                        customTextures.body(),
                        original.cape(),
                        customTextures.elytra(),
                        customTextures.model(),
                        customTextures.secure()
                ));
                return;
            }
        }

        // 2. Cosmetics Engine injection (Local Library.active or Remote Looks.of)
        PlayerSkin original = cir.getReturnValue();
        PlayerSkin modified = CosmeticsEngine.getSkinForPlayer(uuid, isLocal, original);
        if (modified != original) {
            cir.setReturnValue(modified);
        }
    }
}

