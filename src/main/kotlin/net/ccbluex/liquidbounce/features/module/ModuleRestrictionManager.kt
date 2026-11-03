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

package net.ccbluex.liquidbounce.features.module

import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.RefreshArrayListEvent
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.logger
import java.util.concurrent.ConcurrentHashMap

/**
 * Server-Enforced Module Restriction Controller
 * Synchronizes with the backend administration server to prevent unauthorized or disallowed modules
 * from being toggled by restricted accounts.
 */
object ModuleRestrictionManager {

    private val disallowedModules = ConcurrentHashMap.newKeySet<String>()
    var isAccountBanned: Boolean = false
        private set
    var isAccountAdmin: Boolean = false
        private set

    @JvmStatic
    fun isModuleDisallowed(moduleName: String): Boolean {
        if (isAccountBanned) return true
        if (disallowedModules.isEmpty()) return false

        val cleanMod = moduleName.replace(" ", "").lowercase()
        val targetModule = ModuleManager.find { it.name.equals(moduleName, ignoreCase = true) }

        return disallowedModules.any { dis ->
            val cleanDis = dis.replace(" ", "").lowercase()
            cleanMod == cleanDis ||
            cleanMod.contains(cleanDis) ||
            cleanDis.contains(cleanMod) ||
            (targetModule != null && (
                targetModule.aliases.any { it.replace(" ", "").equals(cleanDis, ignoreCase = true) } ||
                (cleanDis == "flight" && cleanMod == "fly") ||
                (cleanDis == "fly" && cleanMod == "flight") ||
                (cleanDis == "aura" && cleanMod == "killaura") ||
                (cleanDis == "killaura" && cleanMod == "aura") ||
                (cleanDis == "jesus" && cleanMod == "waterspeed")
            ))
        }
    }

    @JvmStatic
    fun updateServerRestrictions(modules: Collection<String>, banned: Boolean, admin: Boolean) {
        this.isAccountBanned = banned
        this.isAccountAdmin = admin
        disallowedModules.clear()
        disallowedModules.addAll(modules)

        if (banned) {
            logger.warn("Account is banned from server services.")
            chat("§c[LiquidSecurity] §4Your account has been restricted by server administration.")
            for (module in ModuleManager) {
                if (module.enabled) {
                    module.enabled = false
                }
            }
            EventManager.callEvent(RefreshArrayListEvent)
            return
        }

        if (disallowedModules.isNotEmpty()) {
            logger.info("Enforcing server module restrictions: $disallowedModules")
            for (module in ModuleManager) {
                if (isModuleDisallowed(module.name)) {
                    if (module.enabled) {
                        module.enabled = false
                        chat("§c[LiquidSecurity] §fModule '§e${module.name}§f' has been automatically disabled by server administration.")
                    }
                }
            }
            EventManager.callEvent(RefreshArrayListEvent)
        }
    }

    @JvmStatic
    fun getDisallowed(): Set<String> = disallowedModules
}

