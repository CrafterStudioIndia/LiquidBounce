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

package net.ccbluex.liquidbounce.cosmetics

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.core.HttpClient
import net.ccbluex.liquidbounce.api.core.ioScope
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.kotlin.Minecraft
import net.ccbluex.liquidbounce.utils.render.registerTexture
import net.ccbluex.liquidbounce.utils.render.toNativeImage
import net.minecraft.client.Minecraft
import net.minecraft.core.ClientAsset
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.player.PlayerModelType
import net.minecraft.world.entity.player.PlayerSkin
import okhttp3.Request
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

/**
 * Cosmetics & Skin Engine
 *
 * Intercepts player skin queries for both local client and remote players on servers:
 * - Local player: resolves from [Library.active] and loads from local skin library.
 * - Remote player: checks [Looks.of], downloads asynchronously via [Api.bytes],
 *   processes legacy 64x32 expansion, limb mirroring, and opaque hat layer stripping,
 *   and binds via [Platform.uploadSkin].
 * - Strict offline/privacy guard: blocks any connections to Chinese (.cn / mirror) hosts.
 */
object CosmeticsEngine {

    class SkinTexture(private val identifier: Identifier) : ClientAsset.Texture {
        override fun id(): Identifier = identifier
        override fun texturePath(): Identifier = identifier
    }

    /**
     * Local Skin Library: locates active offline skin files created by launcher or local mods.
     */
    object Library {

        private val localSkinCache = ConcurrentHashMap<String, PlayerSkin>()

        fun active(uuid: UUID): File? {
            val mc = Minecraft.getInstance()
            val gameDir = mc.gameDirectory ?: File(".")
            val playerName = mc.user?.name

            val searchDirs = listOf(
                File(gameDir, "skins"),
                File(gameDir, "CustomSkinLoader/LocalSkin/skins")
            )

            val candidates = mutableListOf<File>()
            for (dir in searchDirs) {
                if (dir.exists()) {
                    candidates.add(File(dir, "$uuid.png"))
                    if (playerName != null) {
                        candidates.add(File(dir, "$playerName.png"))
                    }
                    candidates.add(File(dir, "active.png"))
                    candidates.add(File(dir, "default.png"))
                }
            }
            candidates.add(File(gameDir, "skin.png"))

            return candidates.firstOrNull { it.isFile && it.length() > 0 }
        }

        private fun parseModelFromJson(metaFile: File): PlayerModelType? {
            if (!metaFile.isFile) return null
            return try {
                val json = JsonParser.parseString(metaFile.readText()).asJsonObject
                val model = json.get("model")?.asString ?: json.get("type")?.asString
                if (model.equals("slim", ignoreCase = true)) PlayerModelType.SLIM else PlayerModelType.WIDE
            } catch (_: Exception) {
                null
            }
        }

        fun activeModel(skinFile: File): PlayerModelType {
            val parent = skinFile.parentFile ?: return PlayerModelType.WIDE
            val baseName = skinFile.nameWithoutExtension

            val metaFiles = listOf(
                File(parent, "$baseName.json"),
                File(parent, "active.json"),
                File(parent, "default.json")
            )

            for (metaFile in metaFiles) {
                val model = parseModelFromJson(metaFile)
                if (model != null) return model
            }

            return try {
                val img = ImageIO.read(skinFile)
                if (img != null) Platform.detectModelType(img) else PlayerModelType.WIDE
            } catch (_: Exception) {
                PlayerModelType.WIDE
            }
        }

        fun loadLocalSkin(uuid: UUID): PlayerSkin? {
            val file = active(uuid) ?: return null
            val cacheKey = "${file.absolutePath}:${file.lastModified()}"
            localSkinCache[cacheKey]?.let { return it }

            return try {
                val bytes = file.readBytes()
                val modelType = activeModel(file)
                val identifier = LiquidBounce.identifier("skin-local-${uuid.toString().take(8)}")
                val skin = Platform.uploadSkin(identifier, bytes, modelType)
                localSkinCache[cacheKey] = skin
                skin
            } catch (e: Exception) {
                logger.error("Failed to load local skin for $uuid from ${file.path}", e)
                null
            }
        }
    }

    /**
     * Remote Player Appearances & Skin Metadata
     */
    object Looks {
        class PlayerAppearance(val uuid: UUID) {
            private var skinUrl: String? = null
            private var modelType: PlayerModelType? = null

            fun skin(): String? = skinUrl
            fun model(): PlayerModelType? = modelType

            fun setSkin(url: String?, model: PlayerModelType? = null) {
                this.skinUrl = url
                this.modelType = model
            }
        }

        private val appearances = ConcurrentHashMap<UUID, PlayerAppearance>()
        val skinCache = ConcurrentHashMap<UUID, PlayerSkin>()
        private val pendingRequests = ConcurrentHashMap.newKeySet<UUID>()

        fun of(uuid: UUID): PlayerAppearance {
            return appearances.computeIfAbsent(uuid) { PlayerAppearance(it) }
        }

        fun fetchRemoteSkinAsync(uuid: UUID, url: String, model: PlayerModelType?) {
            if (!pendingRequests.add(uuid)) return

            ioScope.launch {
                try {
                    val bytes = Api.bytes(url)
                    if (bytes != null && bytes.isNotEmpty()) {
                        val identifier = LiquidBounce.identifier("skin-remote-${uuid.toString().take(8)}")
                        val uploaded = Platform.uploadSkin(identifier, bytes, model)
                        skinCache[uuid] = uploaded
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to download or bind remote skin for $uuid from $url", e)
                } finally {
                    pendingRequests.remove(uuid)
                }
            }
        }
    }

    /**
     * Remote Skin API & Network Fetcher
     */
    object Api {
        private val BLOCKED_CHINA_PATTERNS = listOf(
            ".cn", "loyisa", "aliyuncs", "baidupcs", "qq.com", "163.com", "bilibili", "littleskin", "blessingskin"
        )

        /**
         * Reject any connection to China or Chinese domains (.cn, etc.)
         */
        fun isBlockedChina(url: String): Boolean {
            val lower = url.lowercase(Locale.ROOT)
            return BLOCKED_CHINA_PATTERNS.any { lower.contains(it) }
        }

        suspend fun bytes(url: String): ByteArray? {
            if (isBlockedChina(url)) {
                logger.warn("Prevented skin download from blocked Chinese domain/endpoint: $url")
                return null
            }

            return withContext(Dispatchers.IO) {
                try {
                    val request = Request.Builder()
                        .url(url)
                        .get()
                        .header("User-Agent", HttpClient.DEFAULT_AGENT)
                        .build()

                    HttpClient.client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) response.body.bytes() else null
                    }
                } catch (e: IOException) {
                    logger.warn("Failed to fetch skin bytes from $url: ${e.message}")
                    null
                }
            }
        }
    }

    /**
     * Skin Image Processing and Texture Uploading Platform
     */
    object Platform {

        fun detectModelType(img: BufferedImage): PlayerModelType {
            if (img.width >= 64 && img.height >= 64) {
                val alpha1 = (img.getRGB(54, 20) ushr 24) and 0xFF
                val alpha2 = (img.getRGB(55, 20) ushr 24) and 0xFF
                if (alpha1 == 0 || alpha2 == 0) {
                    return PlayerModelType.SLIM
                }
            }
            return PlayerModelType.WIDE
        }

        private fun copyMirroredFace(img: BufferedImage, srcX: Int, srcY: Int, w: Int, h: Int, destX: Int, destY: Int) {
            for (x in 0 until w) {
                for (y in 0 until h) {
                    val pixel = img.getRGB(srcX + (w - 1 - x), srcY + y)
                    img.setRGB(destX + x, destY + y, pixel)
                }
            }
        }

        private fun isHatLayerAllOpaque(img: BufferedImage): Boolean {
            for (x in 32 until 64) {
                for (y in 0 until 16) {
                    val alpha = (img.getRGB(x, y) ushr 24) and 0xFF
                    if (alpha < 255) return false
                }
            }
            return true
        }

        /**
         * Strips opaque solid background from the hat layer (32..63, 0..15) on legacy skins
         * to prevent the solid "black box helmet" rendering artifact.
         */
        @Suppress("CognitiveComplexMethod", "NestedBlockDepth")
        private fun stripOpaqueHatLayer(img: BufferedImage) {
            if (!isHatLayerAllOpaque(img)) return

            val cornerColor = img.getRGB(32, 0)
            var allSame = true
            for (x in 32 until 64) {
                for (y in 0 until 16) {
                    if (img.getRGB(x, y) != cornerColor) {
                        allSame = false
                        break
                    }
                }
                if (!allSame) break
            }

            val targetColor = if (allSame) null else cornerColor
            for (x in 32 until 64) {
                for (y in 0 until 16) {
                    if (targetColor == null || img.getRGB(x, y) == targetColor) {
                        img.setRGB(x, y, 0x00000000)
                    }
                }
            }
        }

        /**
         * Converts legacy 64x32 skin to modern 64x64 format with limb mirroring
         * and opaque hat layer background stripping.
         */
        fun processSkinImage(rawImg: BufferedImage): Pair<BufferedImage, PlayerModelType> {
            val width = rawImg.width
            val height = rawImg.height

            var img = rawImg
            if (width == 64 && height == 32) {
                val expanded = BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB)
                val g = expanded.createGraphics()
                g.drawImage(rawImg, 0, 0, null)
                g.dispose()

                // Mirror right arm (40..55, 16..31) -> left arm (32..47, 48..63)
                copyMirroredFace(expanded, 44, 16, 4, 4, 36, 48) // Top
                copyMirroredFace(expanded, 48, 16, 4, 4, 40, 48) // Bottom
                copyMirroredFace(expanded, 40, 20, 4, 12, 40, 52) // Outer -> Inner
                copyMirroredFace(expanded, 44, 20, 4, 12, 36, 52) // Front
                copyMirroredFace(expanded, 48, 20, 4, 12, 32, 52) // Inner -> Outer
                copyMirroredFace(expanded, 52, 20, 4, 12, 44, 52) // Back

                // Mirror right leg (0..15, 16..31) -> left leg (16..31, 48..63)
                copyMirroredFace(expanded, 4, 16, 4, 4, 20, 48) // Top
                copyMirroredFace(expanded, 8, 16, 4, 4, 24, 48) // Bottom
                copyMirroredFace(expanded, 0, 20, 4, 12, 24, 52) // Outer -> Inner
                copyMirroredFace(expanded, 4, 20, 4, 12, 20, 52) // Front
                copyMirroredFace(expanded, 8, 20, 4, 12, 16, 52) // Inner -> Outer
                copyMirroredFace(expanded, 12, 20, 4, 12, 28, 52) // Back

                // Opaque hat layer stripping so classic Mojang skins render without black box helmets
                stripOpaqueHatLayer(expanded)
                img = expanded
            }

            val modelType = detectModelType(img)
            return Pair(img, modelType)
        }

        fun uploadSkin(
            identifier: Identifier,
            imageBytes: ByteArray,
            modelOverride: PlayerModelType? = null
        ): PlayerSkin {
            val buffered = ImageIO.read(ByteArrayInputStream(imageBytes))
                ?: throw IOException("Invalid skin image data")

            val (processed, detectedModel) = processSkinImage(buffered)
            val finalModel = modelOverride ?: detectedModel

            val nativeImage = processed.toNativeImage()
            val mc = Minecraft.getInstance()
            mc.execute {
                nativeImage.registerTexture(identifier)
            }

            val asset = SkinTexture(identifier)
            return PlayerSkin(
                asset,
                null,
                null,
                finalModel,
                false
            )
        }
    }

    /**
     * Intercepts `getSkin()` for local player or remote player.
     */
    @JvmStatic
    fun getSkinForPlayer(uuid: UUID, isLocal: Boolean, original: PlayerSkin): PlayerSkin {
        if (isLocal) {
            val localSkin = Library.loadLocalSkin(uuid)
            if (localSkin != null) {
                return PlayerSkin(
                    localSkin.body(),
                    original.cape(),
                    original.elytra(),
                    localSkin.model(),
                    localSkin.secure()
                )
            }
            return original
        }

        // Remote player
        val cached = Looks.skinCache[uuid]
        if (cached != null) {
            return PlayerSkin(
                cached.body(),
                original.cape(),
                original.elytra(),
                cached.model(),
                cached.secure()
            )
        }

        val appearance = Looks.of(uuid)
        val skinUrl = appearance.skin()
        if (skinUrl != null) {
            Looks.fetchRemoteSkinAsync(uuid, skinUrl, appearance.model())
        }

        return original
    }
}
