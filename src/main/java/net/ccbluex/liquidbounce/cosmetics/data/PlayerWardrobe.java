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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Storage and controller for client-side local player cosmetics with persistent disk storage.
 */
public final class PlayerWardrobe {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static OutfitConfig activeHeadwear = OutfitConfig.EMPTY;
    private static OutfitConfig activeWings = OutfitConfig.EMPTY;
    private static OutfitConfig activeAura = OutfitConfig.EMPTY;
    private static volatile Integer activeCapeId;
    private static volatile String activeSkinUrl;
    private static volatile boolean activeSkinSlim;
    private static boolean initialized = false;

    static {
        load();
    }

    private PlayerWardrobe() {}

    private static File getConfigFile() {
        Minecraft mc = Minecraft.getInstance();
        File gameDir = (mc != null && mc.gameDirectory != null) ? mc.gameDirectory : new File(".");
        File dir = new File(gameDir, "LiquidBounce");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, "cosmetics.json");
    }

    public static synchronized void load() {
        if (initialized) {
            return;
        }
        initialized = true;
        try {
            File file = getConfigFile();
            if (!file.isFile()) {
                return;
            }
            String jsonText = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(jsonText).getAsJsonObject();

            if (root.has("headwear")) {
                activeHeadwear = GSON.fromJson(root.get("headwear"), OutfitConfig.class);
            }
            if (root.has("wings")) {
                activeWings = GSON.fromJson(root.get("wings"), OutfitConfig.class);
            }
            if (root.has("aura")) {
                activeAura = GSON.fromJson(root.get("aura"), OutfitConfig.class);
            }
            if (root.has("capeId") && !root.get("capeId").isJsonNull()) {
                activeCapeId = root.get("capeId").getAsInt();
            }
            if (activeHeadwear == null) activeHeadwear = OutfitConfig.EMPTY;
            if (activeWings == null) activeWings = OutfitConfig.EMPTY;
            if (activeAura == null) activeAura = OutfitConfig.EMPTY;
        } catch (Exception e) {
            System.err.println("Failed to load cosmetics.json: " + e.getMessage());
        }
    }

    public static synchronized void save() {
        try {
            File file = getConfigFile();
            JsonObject root = new JsonObject();
            root.add("headwear", GSON.toJsonTree(activeHeadwear != null ? activeHeadwear : OutfitConfig.EMPTY));
            root.add("wings", GSON.toJsonTree(activeWings != null ? activeWings : OutfitConfig.EMPTY));
            root.add("aura", GSON.toJsonTree(activeAura != null ? activeAura : OutfitConfig.EMPTY));
            if (activeCapeId != null) {
                root.addProperty("capeId", activeCapeId);
            }

            File temp = new File(file.getParentFile(), "cosmetics.json.tmp");
            Files.writeString(temp.toPath(), GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            System.err.println("Failed to save cosmetics.json: " + e.getMessage());
        }
    }

    public static void clear() {
        activeHeadwear = OutfitConfig.EMPTY;
        activeWings = OutfitConfig.EMPTY;
        activeAura = OutfitConfig.EMPTY;
        activeCapeId = null;
        save();
    }

    public static OutfitConfig getHeadwear() {
        load();
        return activeHeadwear;
    }

    public static OutfitConfig getWings() {
        load();
        return activeWings;
    }

    public static OutfitConfig getAura() {
        load();
        return activeAura;
    }

    public static Integer getCapeId() {
        load();
        return activeCapeId;
    }

    public static PlayerCosmeticTracker.PlayerOutfit getWornOutfit() {
        load();
        return new PlayerCosmeticTracker.PlayerOutfit(
            activeHeadwear,
            activeWings,
            activeAura,
            null,
            activeSkinUrl,
            activeSkinSlim
        );
    }

    public static void setSkinInfo(String skinUrl, boolean isSlim) {
        activeSkinUrl = skinUrl;
        activeSkinSlim = isSlim;
    }

    public static void setHeadwear(OutfitConfig config) {
        activeHeadwear = (config == null) ? OutfitConfig.EMPTY : config;
        save();
    }

    public static void setWings(OutfitConfig config) {
        activeWings = (config == null) ? OutfitConfig.EMPTY : config;
        save();
    }

    public static void setAura(OutfitConfig config) {
        activeAura = (config == null) ? OutfitConfig.EMPTY : config;
        save();
    }

    public static void setCapeId(Integer id) {
        activeCapeId = id;
        save();
    }
}

