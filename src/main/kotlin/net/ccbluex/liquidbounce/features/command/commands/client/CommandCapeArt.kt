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

package net.ccbluex.liquidbounce.features.command.commands.client

import com.google.gson.JsonParser
import kotlinx.coroutines.launch
import net.ccbluex.liquidbounce.api.core.HttpClient
import net.ccbluex.liquidbounce.api.core.ioScope
import net.ccbluex.liquidbounce.cosmetics.CosmeticsEngine
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.module.modules.render.GuiCapeArtScreen
import net.ccbluex.liquidbounce.integration.backend.BrowserBackendManager
import net.ccbluex.liquidbounce.integration.screen.impl.InternetExplorerScreen
import net.ccbluex.liquidbounce.utils.client.browseUrl
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import okhttp3.Request

object CommandCapeArt : Command.Factory {

    override fun createCommand(): Command {
        return CommandBuilder
            .begin("capeart")
            .alias("cape", "capes")
            .hub()
            .handler {
                openCapeScreen()
            }
            .subcommand(openSubcommand())
            .subcommand(menuSubcommand())
            .subcommand(catalogSubcommand())
            .subcommand(equipSubcommand())
            .subcommand(listSubcommand())
            .build()
    }

    private fun openCapeScreen() {
        mc.schedule {
            if (BrowserBackendManager.backend != null) {
                mc.setScreen(InternetExplorerScreen("${CosmeticsEngine.getBackendUrl()}/capes#editor"))
            } else {
                mc.setScreen(GuiCapeArtScreen())
            }
        }
    }

    private fun openSubcommand() = CommandBuilder.begin("open")
        .handler {
            chat(regular("Opening CapeArt Studio..."))
            openCapeScreen()
        }.build()

    private fun menuSubcommand() = CommandBuilder.begin("menu")
        .handler {
            mc.schedule {
                mc.setScreen(GuiCapeArtScreen())
            }
        }.build()

    private fun catalogSubcommand() = CommandBuilder.begin("catalog")
        .handler {
            mc.schedule {
                if (BrowserBackendManager.backend != null) {
                    mc.setScreen(InternetExplorerScreen("${CosmeticsEngine.getBackendUrl()}/capes#catalog"))
                } else {
                    browseUrl("${CosmeticsEngine.getBackendUrl()}/capes#catalog")
                    chat(regular("Opened Cape Catalog in your browser!"))
                }
            }
        }.build()

    private fun equipSubcommand() = CommandBuilder.begin("equip")
        .parameter(
            ParameterBuilder.begin<String>("id")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .required()
                .build()
        )
        .handler {
            val capeId = args[0] as String
            chat(regular("Equipping cape #$capeId..."))
            CosmeticsEngine.equipPublishedCape(capeId) { success, msg ->
                chat(regular(if (success) "Successfully equipped cape #$capeId!" else "Failed to equip cape: $msg"))
            }
        }.build()

    private fun listSubcommand() = CommandBuilder.begin("list")
        .handler {
            chat(regular("Fetching published capes from server..."))
            ioScope.launch {
                try {
                    val req = Request.Builder()
                        .url("${CosmeticsEngine.getBackendUrl()}/api/capes/catalog")
                        .get()
                        .header("User-Agent", HttpClient.DEFAULT_AGENT)
                        .build()

                    HttpClient.client.newCall(req).execute().use { res ->
                        if (res.isSuccessful) {
                            val json = JsonParser.parseString(res.body.string()).asJsonObject
                            val capes = json.getAsJsonArray("capes")
                            if (capes == null || capes.isEmpty) {
                                chat(regular("No approved capes currently available."))
                                return@use
                            }
                            chat(regular("=== Approved Capes ==="))
                            for (c in capes) {
                                val obj = c.asJsonObject
                                val id = obj.get("id").asString
                                val name = obj.get("name").asString
                                val creator = obj.get("creator_name")?.asString ?: "Community"
                                val isHd = obj.get("is_hd")?.asBoolean == true
                                val hdTag = if (isHd) "[HD] " else ""
                                chat(regular("- #$id $hdTag$name by $creator (.capeart equip $id)"))
                            }
                        } else {
                            chat(regular("Failed to fetch cape list: HTTP ${res.code}"))
                        }
                    }
                } catch (e: Exception) {
                    chat(regular("Error contacting server: ${e.message}"))
                }
            }
        }.build()
}

