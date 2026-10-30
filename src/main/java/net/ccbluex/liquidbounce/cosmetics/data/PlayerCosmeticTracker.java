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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks worn outfits and accessory states across local and remote player entities.
 */
public final class PlayerCosmeticTracker {

    public record PlayerOutfit(
        OutfitConfig headwear,
        OutfitConfig wings,
        OutfitConfig aura,
        String capeUrl,
        String skinUrl,
        boolean isSlimModel
    ) {

        public static final PlayerOutfit EMPTY = new PlayerOutfit(
            OutfitConfig.EMPTY,
            OutfitConfig.EMPTY,
            OutfitConfig.EMPTY,
            null,
            null,
            false
        );

        public PlayerOutfit(OutfitConfig headwear, OutfitConfig wings, OutfitConfig aura, String capeUrl) {
            this(headwear, wings, aura, capeUrl, null, false);
        }

        public boolean isEmpty() {
            return headwear.isEmpty() && wings.isEmpty() && aura.isEmpty() && capeUrl == null && skinUrl == null;
        }
    }

    private static final Map<UUID, PlayerOutfit> REMOTE_PLAYER_OUTFITS = new ConcurrentHashMap<>();

    private PlayerCosmeticTracker() {}

    public static PlayerOutfit query(UUID playerId, boolean isSelf) {
        if (isSelf) {
            return PlayerWardrobe.getWornOutfit();
        }
        return REMOTE_PLAYER_OUTFITS.getOrDefault(playerId, PlayerOutfit.EMPTY);
    }

    public static void updateRemotePlayer(UUID playerId, PlayerOutfit outfit) {
        if (outfit == null || outfit.isEmpty()) {
            REMOTE_PLAYER_OUTFITS.remove(playerId);
        } else {
            REMOTE_PLAYER_OUTFITS.put(playerId, outfit);
        }
    }
}

