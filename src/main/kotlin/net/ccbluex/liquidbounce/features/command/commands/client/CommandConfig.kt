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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.api.core.HttpClient
import net.ccbluex.liquidbounce.api.core.HttpMethod
import net.ccbluex.liquidbounce.api.core.parse
import net.ccbluex.liquidbounce.config.autoconfig.AutoConfig
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandExecutor.suspendHandler
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.command.builder.modules
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import org.apache.commons.io.input.CharSequenceReader

/**
 * Config Command
 *
 * Loads a configuration from an external URL.
 */
object CommandConfig : Command.Factory {

    override fun createCommand(): Command {
        return CommandBuilder
            .begin("config")
            .hub()
            .subcommand(loadSubcommand())
            .build()
    }

    private fun loadSubcommand() = CommandBuilder
        .begin("load")
        .parameter(
            ParameterBuilder
                .begin<String>("url")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .required()
                .build()
        )
        .parameter(
            ParameterBuilder.modules()
                .optional()
                .build()
        )
        .suspendHandler {
            val url = args[0] as String
            val modules = args.getOrNull(1) as Set<ClientModule>? ?: emptySet()

            runCatching {
                withContext(Dispatchers.IO) {
                    // Read full response to prevent blocking of Reader
                    HttpClient.request(url, HttpMethod.GET).parse<String>()
                }
            }.onSuccess { source ->
                AutoConfig.withLoading {
                    runCatching {
                        AutoConfig.loadAutoConfig(CharSequenceReader(source), modules)
                    }.onFailure {
                        chat(markAsError(command.result("failedToLoad", variable(url))))
                    }.onSuccess {
                        chat(regular(command.result("loaded", variable(url))))
                    }
                }
            }.onFailure { exception ->
                chat(markAsError(command.result("failedToLoad", variable(url))))
                logger.error("Failed to load config $url", exception)
            }
        }
        .build()

}
