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
import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Works around launchers that shadow the distribution's NSS libraries.
 *
 * AppImage based launchers prepend their own `usr/lib` to `LD_LIBRARY_PATH`, and that directory
 * can contain an older NSS than the system. CEF then ends up mixing both: `libsmime3.so` comes
 * from the system (the launcher does not ship one) while `libnssutil3.so` is taken from the
 * launcher, and loading `libcef.so` fails with
 *
 * ```
 * version `NSSUTIL_3.130' not found (required by /usr/lib/libsmime3.so)
 * ```
 *
 * glibc reuses an already loaded shared object for later dependencies, so preloading one
 * consistent set from the directory the system-provided library lives in fixes the mix-up in the
 * browser process. Chromium's child processes (`jcef_helper`) are launched with `execve` and start
 * with an empty library cache, so they need the same set to be reachable through the search path
 * that is consulted before `LD_LIBRARY_PATH` — see [prepareChildProcesses].
 */
object CefSystemLibraries {

    /**
     * NSS/NSPR libraries in dependency order, as required by Chromium.
     */
    private val LIBRARIES = listOf(
        "libnspr4.so",
        "libplc4.so",
        "libplds4.so",
        "libnssutil3.so",
        "libnss3.so",
        "libsmime3.so",
    )

    private val SYSTEM_DIRECTORIES = listOf(
        "/usr/lib/x86_64-linux-gnu",
        "/usr/lib/aarch64-linux-gnu",
        "/usr/lib64",
        "/usr/lib",
        "/lib64",
        "/lib",
    )

    /**
     * Directory holding the consistent NSS/NSPR set, or null when the loader would not mix sets.
     *
     * The system copy of `libsmime3.so` is what pulls in a newer `libnssutil3.so`, so its directory
     * is the one that has to be used consistently.
     */
    private val mixedSystemDirectory: File? by lazy(::resolveMixedSystemDirectory)

    private var preloadAttempted = false

    /**
     * Preloads the system NSS libraries when the loader would otherwise mix library directories.
     */
    fun preloadIfMixed() {
        if (preloadAttempted || !isLinux()) {
            return
        }

        preloadAttempted = true
        val systemDirectory = mixedSystemDirectory ?: return

        logger.warn(
            "Mixed NSS libraries detected; preloading the set from {} so JCEF can load.",
            systemDirectory.path
        )

        LIBRARIES.forEach { library ->
            val file = File(systemDirectory, library)

            if (file.isFile) {
                runCatching { System.load(file.absolutePath) }.onFailure {
                    logger.warn("Failed to preload {}", file.path, it)
                }
            }
        }
    }

    /**
     * Makes the NSS set reachable from CEF's child processes.
     *
     * The extracted `jcef_helper` searches its own directory first (`DT_RPATH`), but `libcef.so`
     * declares `$ORIGIN` as `DT_RUNPATH`, which glibc only consults *after* `LD_LIBRARY_PATH` —
     * so the launcher's older NSS would win again in every child process. Copying the consistent
     * set next to the natives and marking it as `DT_RPATH` puts it first for parents and children
     * alike.
     */
    fun prepareChildProcesses(nativesDirectory: File) {
        if (!isLinux() || !nativesDirectory.isDirectory) {
            return
        }

        val systemDirectory = mixedSystemDirectory ?: return

        logger.warn(
            "Mixed NSS libraries detected; copying the set from {} into {} for CEF's child processes.",
            systemDirectory.path,
            nativesDirectory.path
        )

        if (!copyLibraries(systemDirectory, nativesDirectory)) {
            logger.warn(
                "Could not place the NSS set in {}, child processes may still pick up the launcher's.",
                nativesDirectory.path
            )
            return
        }

        val cefLibrary = File(nativesDirectory, "libcef.so")
        if (ElfRpath.preferLocalLibraries(cefLibrary)) {
            logger.info("Marked {} as DT_RPATH so child processes see the copied NSS set.", cefLibrary.path)
        }
    }

    private fun isLinux() = MCEFPlatform.getPlatform().isLinux

    private fun resolveMixedSystemDirectory(): File? {
        val launcherDirectories = System.getenv("LD_LIBRARY_PATH")
            .orEmpty()
            .split(File.pathSeparatorChar)
            .filter(String::isNotBlank)

        val resolved = LIBRARIES.associateWith { library ->
            (launcherDirectories + SYSTEM_DIRECTORIES)
                .map { File(it, library) }
                .firstOrNull(File::isFile)
                ?.canonicalFile
        }

        val directories = resolved.values
            .filterNotNull()
            .map(File::getParentFile)
            .filterNotNull()
            .toSet()

        return if (directories.size <= 1) {
            null
        } else {
            resolved["libsmime3.so"]?.parentFile
        }
    }

    /**
     * @return true when the natives directory holds the complete set afterwards
     */
    private fun copyLibraries(systemDirectory: File, nativesDirectory: File): Boolean {
        LIBRARIES.forEach { library ->
            val source = File(systemDirectory, library)
            val target = File(nativesDirectory, library)

            if (!source.isFile || !needsCopy(source, target)) {
                return@forEach
            }

            runCatching {
                Files.copy(
                    source.toPath(),
                    target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.COPY_ATTRIBUTES
                )
            }.onFailure {
                logger.warn("Failed to copy {} into the JCEF natives directory", source.path, it)
            }
        }

        return LIBRARIES.all { File(nativesDirectory, it).isFile }
    }

    private fun needsCopy(source: File, target: File) =
        !target.isFile || target.length() != source.length() || target.lastModified() < source.lastModified()

}

/**
 * Minimal ELF64 reader/writer that changes how the dynamic loader searches for the dependencies of
 * `libcef.so`.
 */
private object ElfRpath {

    private const val ELF_MAGIC = 0x7F454C46
    private const val ELF_CLASS_64 = 2
    private const val ELF_DATA_LSB = 1
    private const val ELF_HEADER_SIZE = 0x40L
    private const val ELF_PROGRAM_HEADER_OFFSET = 0x20L
    private const val ELF_PROGRAM_HEADER_ENTRY_SIZE_OFFSET = 0x36L
    private const val PROGRAM_HEADER_TYPE_DYNAMIC = 2
    private const val DYNAMIC_ENTRY_SIZE = 16L
    private const val DYNAMIC_NULL = 0L
    private const val DYNAMIC_RPATH = 0x0FL
    private const val DYNAMIC_RUNPATH = 0x1DL

    /**
     * Rewrites the `DT_RUNPATH` entry of the dynamic section to `DT_RPATH`.
     *
     * Both tags share the same string table entry, so only the tag has to change. RPATH is the
     * only one of the two that is searched before `LD_LIBRARY_PATH`.
     *
     * @return true when the library was patched, false when it already is or is not ELF64
     */
    fun preferLocalLibraries(library: File): Boolean {
        if (!library.isFile) {
            return false
        }

        return runCatching {
            RandomAccessFile(library, "rw").use(::switchToRpath)
        }.getOrElse {
            logger.warn("Failed to inspect {}", library.path, it)
            false
        }
    }

    private fun switchToRpath(file: RandomAccessFile): Boolean {
        file.seek(0)
        if (file.length() < ELF_HEADER_SIZE || file.readInt() != ELF_MAGIC) {
            return false
        }

        file.seek(4)
        if (file.readUnsignedByte() != ELF_CLASS_64 || file.readUnsignedByte() != ELF_DATA_LSB) {
            return false
        }

        file.seek(ELF_PROGRAM_HEADER_OFFSET)
        val programHeaderOffset = readLong(file)

        file.seek(ELF_PROGRAM_HEADER_ENTRY_SIZE_OFFSET)
        val entrySize = readShort(file)
        val entryCount = readShort(file)

        for (index in 0 until entryCount) {
            val header = programHeaderOffset + index.toLong() * entrySize

            file.seek(header)
            if (readInt(file) != PROGRAM_HEADER_TYPE_DYNAMIC) {
                continue
            }

            file.seek(header + 8)
            val offset = readLong(file)
            file.skipBytes(16)
            val size = readLong(file)

            return patchDynamicSection(file, offset, size)
        }

        return false
    }

    private fun patchDynamicSection(file: RandomAccessFile, offset: Long, size: Long): Boolean {
        var position = offset

        while (position + DYNAMIC_ENTRY_SIZE <= offset + size) {
            file.seek(position)

            when (readLong(file)) {
                DYNAMIC_RUNPATH -> {
                    file.seek(position)
                    writeLong(file, DYNAMIC_RPATH)
                    return true
                }
                DYNAMIC_NULL -> return false
            }

            position += DYNAMIC_ENTRY_SIZE
        }

        return false
    }

    private fun readLong(file: RandomAccessFile) = java.lang.Long.reverseBytes(file.readLong())

    private fun writeLong(file: RandomAccessFile, value: Long) = file.writeLong(java.lang.Long.reverseBytes(value))

    private fun readInt(file: RandomAccessFile) = Integer.reverseBytes(file.readInt())

    private fun readShort(file: RandomAccessFile) = java.lang.Short.reverseBytes(file.readShort()).toInt() and 0xFFFF

}
