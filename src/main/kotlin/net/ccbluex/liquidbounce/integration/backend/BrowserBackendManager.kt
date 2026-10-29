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
package net.ccbluex.liquidbounce.integration.backend

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.BrowserReadyEvent
import net.ccbluex.liquidbounce.event.events.GameRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.integration.backend.backends.cef.CefBrowserBackend
import net.ccbluex.liquidbounce.integration.backend.backends.cef.CefNativeBundle
import net.ccbluex.liquidbounce.integration.backend.backends.external.ExternalSystemBrowserBackend
import net.ccbluex.liquidbounce.integration.backend.browser.GlobalBrowserSettings
import net.ccbluex.liquidbounce.integration.interop.persistant.PersistentLocalStorage
import net.ccbluex.liquidbounce.integration.task.TaskManager
import net.ccbluex.liquidbounce.utils.client.env
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

object BrowserBackendManager : EventListener {

    private val logger: Logger = LogManager.getLogger("$CLIENT_NAME/BrowserBackendManager")

    val isInitialized: Boolean
        get() = backend?.isInitialized ?: false
    var backend: BrowserBackend? = null

    var isSkipping = env("LB_BROWSER_SKIP", "net.ccbluex.liquidbounce.browser.skip")?.toBoolean()
        ?: false

    private var taskManager: TaskManager? = null
    val backendName = env("LB_BROWSER_BACKEND", "net.ccbluex.liquidbounce.browser.backend") ?: "cef"
    val disableAcceleration = env("LB_BROWSER_DISABLE_ACCELERATION",
        "net.ccbluex.liquidbounce.browser.disableAcceleration")?.toBoolean() ?: false

    fun init() {
        PersistentLocalStorage
    }

    /**
     * Makes the browser dependencies available and initializes the browser
     * when the dependencies are available.
     */
    fun makeDependenciesAvailable(taskManager: TaskManager) {
        this.taskManager = taskManager

        if (isSkipping) {
            logger.warn("Environment variable 'LB_BROWSER_SKIP' is set to 'true'.")
            return
        }

        val browserBackend = when (backendName) {
            "none" -> {
                logger.warn("Environment variable 'LB_BROWSER_BACKEND' is set to 'none'.")
                isSkipping = true
                return
            }
            "cef" -> if (CefNativeBundle.isAvailable()) {
                CefBrowserBackend()
            } else {
                logger.warn(
                    "No JCEF natives bundled for platform '{}'. Falling back to the external browser backend.",
                    CefNativeBundle.platformName
                )
                ExternalSystemBrowserBackend()
            }
            "external" -> ExternalSystemBrowserBackend()
            else -> error("Unknown browser backend: $backendName")
        }
        this.backend = browserBackend
        browserBackend.makeDependenciesAvailable(taskManager, ::start)
    }

    /**
     * Initializes the browser.
     */
    fun start() {
        // Ensure that the browser is available
        logger.info("Initializing browser...")

        // Ensure that the browser is started on the render thread
        RenderSystem.assertOnRenderThread()

        val browserBackend = backend ?: return

        if (!startBackend(browserBackend)) {
            // The fallback backend re-entered this method and completed the startup
            return
        }

        if (disableAcceleration) {
            logger.warn("Environment variable 'LB_BROWSER_DISABLE_ACCELERATION' is set to 'true'.")
        }
        GlobalBrowserSettings
        EventManager.callEvent(BrowserReadyEvent)
        logger.info("Successfully initialized browser.")
    }

    /**
     * Starts [browserBackend] and falls back to the external browser when JCEF cannot be loaded.
     *
     * @return false when the backend was replaced, the replacement already performed the startup
     */
    private fun startBackend(browserBackend: BrowserBackend): Boolean {
        try {
            browserBackend.start()
            return true
        } catch (linkageError: LinkageError) {
            if (browserBackend !is CefBrowserBackend) {
                throw linkageError
            }

            logger.error(
                "JCEF could not load its native libraries, falling back to the external browser. " +
                    "Launch the game without the launcher's LD_LIBRARY_PATH or install the JCEF " +
                    "dependencies (nss, nspr, gtk3) to get the in-game GUI back.",
                linkageError
            )
        }

        val fallback = ExternalSystemBrowserBackend()
        backend = fallback
        fallback.makeDependenciesAvailable(taskManager ?: return false, ::start)

        return false
    }

    /**
     * Shuts down the browser.
     */
    fun stop() = runCatching {
        backend?.stop()
    }.onFailure {
        logger.error("Failed to shutdown browser.", it)
    }.onSuccess {
        logger.info("Successfully shutdown browser.")
    }

    /**
     * Causes an update of every browser by re-setting their viewport.
     */
    fun forceUpdate() = mc.execute {
        val browserBackend = backend ?: return@execute

        for (browser in browserBackend.browsers) {
            try {
                browser.viewport = browser.viewport
            } catch (e: Exception) {
                logger.error("Failed to update tab of '${browser.url}'", e)
            }
        }
    }

    @Suppress("unused")
    private val gameRenderHandler = handler<GameRenderEvent>(priority = FIRST_PRIORITY) {
        val browserBackend = backend ?: return@handler
        if (!browserBackend.isInitialized) {
            return@handler
        }

        browserBackend.update()
    }

}
