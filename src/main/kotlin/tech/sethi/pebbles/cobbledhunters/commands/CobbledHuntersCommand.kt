package tech.sethi.pebbles.cobbledhunters.commands

import com.velocitypowered.api.command.BrigadierCommand
import tech.sethi.pebbles.cobbledhunters.hunt.GlobalHuntHandler
import tech.sethi.pebbles.cobbledhunters.util.GlobalResources.server
import tech.sethi.pebbles.cobbledhunters.util.PM
import tech.sethi.pebbles.cobbledhunters.util.PermUtil
import tech.sethi.pebbles.cobbledhunters.util.literal

object CobbledHuntersCommand {

    fun registerCommands(pluginInstance: Any) {
        val commandManager = server.commandManager

        val huntadminCommand = literal("huntadmin").requires { source ->
            PermUtil.commandSourceRequiresPermission(
                source, "pebbles.cobbledhunters.admin"
            )
        }

        val reloadCommand = literal("globalreload").executes { context ->
            GlobalHuntHandler.reloadPools()

            context.source.forEachAudience { it.sendMessage(PM.returnStyledText("Global pools reloaded!")) }

            1
        }

        val huntadminMeta = commandManager.metaBuilder("huntadmin").aliases("ha").plugin(pluginInstance).build()

        huntadminCommand.then(reloadCommand)

        commandManager.register(huntadminMeta, BrigadierCommand(huntadminCommand))
    }

}