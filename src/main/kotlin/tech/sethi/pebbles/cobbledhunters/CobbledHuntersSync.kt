package tech.sethi.pebbles.cobbledhunters

import com.google.inject.Inject
import com.mongodb.client.model.Filters
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.ServerConnectedEvent
import com.velocitypowered.api.event.proxy.ListenerCloseEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import kotlinx.coroutines.*
import tech.sethi.pebbles.cobbledhunters.cio.CIOApplication
import tech.sethi.pebbles.cobbledhunters.commands.CobbledHuntersCommand
import tech.sethi.pebbles.cobbledhunters.db.DatabaseHandler
import tech.sethi.pebbles.cobbledhunters.hunt.GlobalHuntHandler
import tech.sethi.pebbles.cobbledhunters.hunt.type.Participant
import tech.sethi.pebbles.cobbledhunters.hunt.type.RewardStorage
import tech.sethi.pebbles.cobbledhunters.util.config.ConfigHandler
import tech.sethi.pebbles.cobbledhunters.util.GlobalResources

@Plugin(
    id = "pebbles_cobbledhunters_sync",
    name = "Pebble's Cobbled Hunters Sync",
    version = "1.0-SNAPSHOT",
    authors = ["pebbles"]
)
class CobbledHuntersSync @Inject constructor(server: ProxyServer, logger: org.slf4j.Logger) {

    init {
        GlobalResources.initialize(server, logger)

        ConfigHandler
        GlobalHuntHandler

        CobbledHuntersCommand.registerCommands(this)

        CoroutineScope(Dispatchers.IO).launch {
            CIOApplication.main()
        }

        logger.info("Pebble's Cobbled Hunters Sync Initialized!")
    }

    @Subscribe(order = PostOrder.NORMAL)
    fun onPlayerJoin(event: ServerConnectedEvent) {
        val player = event.player
        val participant = Participant(player.uniqueId.toString(), player.username)

        if (DatabaseHandler.playerRewardStorageCollection.find(
                Filters.eq("playerUUID", participant.uuid)
            ).first() == null
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                DatabaseHandler.playerRewardStorageCollection.insertOne(
                    RewardStorage(
                        participant.uuid, participant.name, mutableListOf()
                    )
                )
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            delay(1000)
            val tracker = GlobalHuntHandler.participatingHunt(participant) ?: return@launch
            val poolId = GlobalHuntHandler.globalHuntPools.entries.find { it.value == tracker }?.key ?: return@launch
            GlobalHuntHandler.addPlayerToBossBar(player.uniqueId.toString(), poolId)
        }
    }

    @Subscribe(order = PostOrder.NORMAL)
    fun onServerClose(event: ListenerCloseEvent) {
        GlobalResources.logger.info("Server closing")
        GlobalResources.logger.info(event.address.hostName)
        GlobalResources.logger.info(event.address.port.toString())
        GlobalResources.logger.info(GlobalResources.authenticatedServers.toString())
        GlobalResources.logger.info(GlobalResources.cobbledHuntersSession.toString())
    }
}

