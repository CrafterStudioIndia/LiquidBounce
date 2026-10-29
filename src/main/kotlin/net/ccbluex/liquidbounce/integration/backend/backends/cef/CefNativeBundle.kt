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
package net.ccbluex.liquidbounce.integration.backend.backends.cef

import net.ccbluex.liquidbounce.mcef.MCEFPlatform
import net.ccbluex.liquidbounce.utils.client.logger
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.zip.GZIPInputStream

/**
 * Ships the Chromium Embedded Framework binaries inside the mod jar instead of downloading them
 * from the upstream CDN at runtime.
 *
 * The archives are the unmodified upstream release artifacts for the JCEF commit used by the
 * bundled MCEF version. They are verified against a pinned SHA-256 before being extracted and are
 * unpacked into the directory MCEF expects (`<libraries>/<commit>/<platform>`), after which MCEF
 * never has to touch the network.
 */
@Suppress("TooManyFunctions")
object CefNativeBundle {

    private const val BUNDLE_DIRECTORY = "/resources/liquidbounce/cef"
    private const val MARKER_FILE = ".verified-bundle"
    private const val TAR_BLOCK_SIZE = 512
    private const val DIRECTORY_TYPE = '5'
    private const val FILE_TYPE = '0'
    private const val LEGACY_FILE_TYPE = '\u0000'

    /**
     * Platform name (as reported by [MCEFPlatform.normalizedName]) to the archive's SHA-256.
     */
    private val BUNDLES = mapOf(
        "linux_amd64" to "780293eec1cc90330a4895d57a8562a222b2329a5baef8ff992025d362c2e267",
        "windows_amd64" to "099036fcd390340de78f3fe9df992bd5418f67456512848752057ac029057fb2",
    )

    val platformName: String
        get() = MCEFPlatform.getPlatform().normalizedName

    /**
     * Whether a native bundle for [platform] is embedded in this jar.
     */
    fun isAvailable(platform: String = platformName): Boolean =
        BUNDLES.containsKey(platform) && openBundle(platform) != null

    /**
     * Makes sure the natives are extracted to [target] (MCEF's platform directory).
     *
     * @return true when the natives are ready to be loaded
     */
    fun install(target: File, platform: String = platformName): Boolean {
        val expectedHash = BUNDLES[platform] ?: return false

        if (isInstalled(target, expectedHash)) {
            return true
        }

        val archive = openBundle(platform) ?: return false
        val parent = target.absoluteFile.parentFile
            ?: return false
        val temporary = File(parent, "${target.name}.tmp")

        temporary.deleteRecursively()
        if (!temporary.mkdirs()) {
            logger.warn("Failed to create directory ${temporary.absolutePath}")
            return false
        }

        return try {
            val digest = MessageDigest.getInstance("SHA-256")

            archive.use { raw ->
                DigestInputStream(BufferedInputStream(raw), digest).use { digested ->
                    GZIPInputStream(digested).use { gzip ->
                        extractTar(gzip, temporary, platform)
                    }
                }
            }

            val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
            if (actualHash != expectedHash) {
                logger.error("JCEF bundle for $platform failed verification ($actualHash)")
                temporary.deleteRecursively()
                return false
            }

            target.deleteRecursively()
            if (!temporary.renameTo(target)) {
                temporary.copyRecursively(target, overwrite = true)
                temporary.deleteRecursively()
            }

            File(target, MARKER_FILE).writeText(expectedHash)
            logger.info("Extracted JCEF natives for $platform to ${target.absolutePath}")
            true
        } catch (exception: IOException) {
            logger.error("Failed to extract JCEF natives for $platform", exception)
            temporary.deleteRecursively()
            false
        }
    }

    private fun isInstalled(target: File, expectedHash: String): Boolean {
        val marker = File(target, MARKER_FILE)

        return marker.isFile && runCatching { marker.readText().trim() }.getOrNull() == expectedHash
    }

    private fun openBundle(platform: String): InputStream? =
        CefNativeBundle::class.java.getResourceAsStream("$BUNDLE_DIRECTORY/$platform.tar.gz")

    private data class TarEntry(val name: String, val size: Long, val type: Char)

    /**
     * Minimal ustar reader, the bundles only contain regular files and directories.
     */
    private fun extractTar(input: InputStream, root: File, topLevel: String) {
        val header = ByteArray(TAR_BLOCK_SIZE)

        while (true) {
            if (!input.readFully(header, TAR_BLOCK_SIZE) || header.all { it == 0.toByte() }) {
                return
            }

            val entry = parseHeader(header) ?: return
            val relative = entry.name.removePrefix("$topLevel/")

            if (isUnsafe(relative)) {
                input.skipFully(entry.size + padding(entry.size))
                continue
            }

            if (entry.type == DIRECTORY_TYPE) {
                File(root, relative).mkdirs()
            } else if (entry.type == FILE_TYPE || entry.type == LEGACY_FILE_TYPE) {
                writeEntry(input, File(root, relative), entry)
            } else {
                input.skipFully(entry.size)
            }

            input.skipFully(padding(entry.size))
        }
    }

    private fun parseHeader(header: ByteArray): TarEntry? {
        val name = header.readString(0, 100)
        if (name.isEmpty()) {
            return null
        }

        val size = header.readString(124, 12).trim().ifEmpty { "0" }.toLong(8)

        return TarEntry(name, size, header[156].toInt().toChar())
    }

    private fun writeEntry(input: InputStream, output: File, entry: TarEntry) {
        output.parentFile?.mkdirs()

        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var remaining = entry.size

        output.outputStream().use { stream ->
            while (remaining > 0) {
                val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                if (read == -1) {
                    throw IOException("Unexpected end of ${entry.name}")
                }

                stream.write(buffer, 0, read)
                remaining -= read
            }
        }
    }

    private fun isUnsafe(relative: String): Boolean =
        relative.isEmpty() || relative.startsWith('/') || relative.split('/').contains("..")

    private fun padding(size: Long): Long =
        if (size % TAR_BLOCK_SIZE == 0L) 0 else TAR_BLOCK_SIZE - size % TAR_BLOCK_SIZE

    private fun ByteArray.readString(offset: Int, length: Int): String {
        var end = offset
        while (end < offset + length && this[end] != 0.toByte()) {
            end++
        }

        return String(this, offset, end - offset, Charsets.US_ASCII)
    }

    private fun InputStream.readFully(target: ByteArray, length: Int): Boolean {
        var read = 0

        while (read < length) {
            val current = read(target, read, length - read)
            if (current == -1) {
                return false
            }

            read += current
        }

        return true
    }

    private fun InputStream.skipFully(length: Long) {
        var remaining = length

        while (remaining > 0) {
            val skipped = skip(remaining)
            if (skipped <= 0) {
                if (read() == -1) {
                    return
                }

                remaining--
                continue
            }

            remaining -= skipped
        }
    }

}
