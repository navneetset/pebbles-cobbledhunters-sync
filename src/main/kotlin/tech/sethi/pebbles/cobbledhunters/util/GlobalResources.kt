package tech.sethi.pebbles.cobbledhunters.util

import com.velocitypowered.api.proxy.ProxyServer
import io.ktor.websocket.*
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

object GlobalResources {
    lateinit var server: ProxyServer
    lateinit var logger: org.slf4j.Logger

    val cobbledHuntersSession = ConcurrentHashMap<String, WebSocketSession>()
    val authenticatedServers = mutableSetOf<String>()

    fun initialize(server: ProxyServer, logger: org.slf4j.Logger) {
        this.server = server
        this.logger = logger

        CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                cobbledHuntersSession.forEach {
                    // if session is no longer active, remove it
                    if (it.value.isActive.not()) {
                        authenticatedServers.remove(it.key)
                        cobbledHuntersSession.remove(it.key)
                        GlobalResources.logger.info("Server ${it.key} disconnected from Cobbled Hunters socket!")
                    }

                }
                delay(1000)
            }
        }
    }
}
