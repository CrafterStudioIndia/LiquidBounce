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

@file:Suppress("CognitiveComplexMethod", "NestedBlockDepth", "ReturnCount", "TooGenericExceptionCaught")

package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.netty.handler.codec.http.FullHttpResponse
import net.ccbluex.liquidbounce.cosmetics.CosmeticsEngine
import net.ccbluex.liquidbounce.cosmetics.data.OutfitConfig
import net.ccbluex.liquidbounce.cosmetics.data.PlayerWardrobe
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpBadRequest
import net.ccbluex.netty.http.util.httpNoContent
import net.ccbluex.netty.http.util.httpOk
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Base64
import java.util.Locale

private fun getSkinsDir(): File {
    val gameDir = mc.gameDirectory ?: File(".")
    val dir = File(gameDir, "skins")
    if (!dir.exists()) {
        dir.mkdirs()
    }
    return dir
}

// GET /api/v1/client/skins
@Suppress("UNUSED_PARAMETER")
fun getSkins(requestObject: RequestObject): FullHttpResponse {
    val dir = getSkinsDir()
    val activeFile = File(dir, "active.png")
    val activeMeta = File(dir, "active.json")

    var currentModel = "classic"
    if (activeMeta.isFile) {
        try {
            val json = JsonParser.parseString(activeMeta.readText()).asJsonObject
            val m = json.get("model")?.asString ?: json.get("type")?.asString
            if (m.equals("slim", ignoreCase = true)) {
                currentModel = "slim"
            }
        } catch (_: Exception) {
            // keep classic
        }
    }

    val skinList = JsonArray()
    val home = System.getProperty("user.home") ?: ""
    val searchDirs = mutableListOf(
        dir,
        File(dir, "library"),
        File(mc.gameDirectory ?: File("."), "CustomSkinLoader/LocalSkin/skins")
    )
    if (home.isNotEmpty()) {
        searchDirs.add(File(home, ".config/liquidlauncher/skins"))
        searchDirs.add(File(home, ".config/net.CCBlueX.LiquidLauncher/skins"))
        searchDirs.add(File(home, ".local/share/liquidlauncher/skins"))
        searchDirs.add(File(home, ".local/share/LiquidLauncher/skins"))
        searchDirs.add(File(home, ".minecraft/skins"))
    }

    val seenFiles = mutableSetOf<String>()
    for (searchDir in searchDirs) {
        if (!searchDir.isDirectory) continue

        // Parse skins.json if available
        val metaMap = mutableMapOf<String, Pair<String, String>>()
        val indexJson = File(searchDir, "skins.json")
        if (indexJson.isFile) {
            try {
                val root = JsonParser.parseString(indexJson.readText()).asJsonObject
                val array = root.getAsJsonArray("skins")
                if (array != null) {
                    for (elem in array) {
                        val obj = elem.asJsonObject
                        val fn = obj.get("fileName")?.asString ?: continue
                        val n = obj.get("name")?.asString ?: fn
                        val m = obj.get("model")?.asString ?: "classic"
                        metaMap[fn.lowercase()] = Pair(n, m)
                    }
                }
            } catch (_: Exception) {}
        }

        val files = searchDir.listFiles { f -> f.isFile && f.extension.equals("png", ignoreCase = true) } ?: continue
        for (f in files) {
            val name = f.nameWithoutExtension
            if (name.equals("active", ignoreCase = true)) continue
            if (!seenFiles.add(f.absolutePath)) continue

            val metaInfo = metaMap[f.name.lowercase()]
            val displayName = metaInfo?.first ?: name
            var model = metaInfo?.second ?: "classic"

            val metaFile = File(f.parentFile, "$name.json")
            if (metaFile.isFile) {
                try {
                    val j = JsonParser.parseString(metaFile.readText()).asJsonObject
                    if ((j.get("model")?.asString ?: j.get("type")?.asString)?.equals("slim", true) == true) {
                        model = "slim"
                    }
                } catch (_: Exception) {
                    // ignore
                }
            }

            skinList.add(JsonObject().apply {
                addProperty("id", f.name)
                addProperty("name", displayName)
                addProperty("file", f.absolutePath)
                addProperty("model", model)
            })
        }
    }

    val response = JsonObject().apply {
        addProperty("active", if (activeFile.isFile) "active.png" else "")
        addProperty("model", currentModel)
        addProperty("hasActive", activeFile.isFile && activeFile.length() > 0)
        add("skins", skinList)
    }

    return httpOk(response)
}

// POST /api/v1/client/skins/select
fun selectSkin(requestObject: RequestObject): FullHttpResponse {
    data class SelectBody(val file: String?, val model: String?, val base64: String?)
    val body = runCatching { requestObject.asJson<SelectBody>() }.getOrNull()
        ?: return httpBadRequest("Invalid request body")

    val dir = getSkinsDir()
    val activeFile = File(dir, "active.png")
    val activeMeta = File(dir, "active.json")

    val model = if (body.model?.equals("slim", ignoreCase = true) == true) "slim" else "classic"

    if (!body.base64.isNullOrEmpty()) {
        try {
            val clean = body.base64.substringAfter("base64,")
            val bytes = Base64.getDecoder().decode(clean)
            activeFile.writeBytes(bytes)
        } catch (e: Exception) {
            return httpBadRequest("Failed to decode base64: ${e.message}")
        }
    } else if (!body.file.isNullOrEmpty()) {
        val src = File(body.file)
        if (!src.isFile) {
            return httpBadRequest("Source skin file not found: ${body.file}")
        }
        Files.copy(src.toPath(), activeFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        val localCopy = File(dir, src.name)
        if (!localCopy.exists()) {
            runCatching { Files.copy(src.toPath(), localCopy.toPath(), StandardCopyOption.REPLACE_EXISTING) }
        }
    } else {
        return httpBadRequest("Missing file or base64 parameter")
    }

    activeMeta.writeText("{\"model\":\"$model\"}")
    CosmeticsEngine.Library.clearCache()
    return httpNoContent()
}

// POST /api/v1/client/skins/model
fun setSkinModel(requestObject: RequestObject): FullHttpResponse {
    data class ModelBody(val model: String)
    val body = runCatching { requestObject.asJson<ModelBody>() }.getOrNull()
        ?: return httpBadRequest("Invalid request body")

    val dir = getSkinsDir()
    val activeMeta = File(dir, "active.json")
    val model = if (body.model.equals("slim", ignoreCase = true)) "slim" else "classic"
    activeMeta.writeText("{\"model\":\"$model\"}")
    CosmeticsEngine.Library.clearCache()
    return httpNoContent()
}

// POST /api/v1/client/skins/reset
@Suppress("UNUSED_PARAMETER")
fun resetSkin(requestObject: RequestObject): FullHttpResponse {
    val dir = getSkinsDir()
    File(dir, "active.png").delete()
    File(dir, "active.json").delete()
    CosmeticsEngine.Library.clearCache()
    return httpNoContent()
}

// GET /api/v1/client/cosmetics
@Suppress("UNUSED_PARAMETER")
fun getCosmetics(requestObject: RequestObject): FullHttpResponse {
    val aura = PlayerWardrobe.getAura()
    val wings = PlayerWardrobe.getWings()
    val headwear = PlayerWardrobe.getHeadwear()

    val response = JsonObject().apply {
        add("aura", JsonObject().apply {
            addProperty("style", aura.styleId())
            addProperty("primary", aura.primaryColor())
            addProperty("secondary", aura.secondaryColor())
            addProperty("lift", aura.verticalOffset())
            addProperty("glow", aura.glowFactor())
        })
        add("wings", JsonObject().apply {
            addProperty("style", wings.styleId())
            addProperty("primary", wings.primaryColor())
            addProperty("secondary", wings.secondaryColor())
            addProperty("lift", wings.verticalOffset())
            addProperty("glow", wings.glowFactor())
        })
        add("headwear", JsonObject().apply {
            addProperty("style", headwear.styleId())
            addProperty("primary", headwear.primaryColor())
            addProperty("secondary", headwear.secondaryColor())
            addProperty("lift", headwear.verticalOffset())
            addProperty("glow", headwear.glowFactor())
        })
    }
    return httpOk(response)
}

// POST /api/v1/client/cosmetics
fun setCosmetics(requestObject: RequestObject): FullHttpResponse {
    data class CosmeticReq(
        val category: String,
        val style: String?,
        val primary: Int?,
        val secondary: Int?,
        val lift: Float?,
        val glow: Float?
    )
    val body = runCatching { requestObject.asJson<CosmeticReq>() }.getOrNull()
        ?: return httpBadRequest("Invalid cosmetic request")

    val style = body.style ?: ""
    val p = body.primary ?: 0xFFFFFF
    val sec = body.secondary ?: 0xFFFFFF
    val l = body.lift ?: 0.0f
    val g = body.glow ?: 0.5f
    val cfg = if (style.isEmpty()) OutfitConfig.EMPTY else OutfitConfig(style, p, sec, l, g)

    when (body.category.lowercase(Locale.ROOT)) {
        "aura" -> PlayerWardrobe.setAura(cfg)
        "wings" -> PlayerWardrobe.setWings(cfg)
        "headwear", "hat" -> PlayerWardrobe.setHeadwear(cfg)
        else -> return httpBadRequest("Unknown cosmetic category")
    }
    return httpNoContent()
}
