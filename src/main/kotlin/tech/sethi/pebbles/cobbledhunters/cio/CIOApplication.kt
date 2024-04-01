package tech.sethi.pebbles.cobbledhunters.cio

import com.google.gson.Gson
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tech.sethi.pebbles.cobbledhunters.cio.sockets.SocketMessage
import tech.sethi.pebbles.cobbledhunters.cio.sockets.configureWebsocket
import tech.sethi.pebbles.cobbledhunters.util.config.ConfigHandler
import tech.sethi.pebbles.cobbledhunters.util.GlobalResources
import tech.sethi.pebbles.cobbledhunters.util.GlobalResources.authenticatedServers
import tech.sethi.pebbles.cobbledhunters.util.GlobalResources.cobbledHuntersSession


fun Application.module() {
    routing {
        get("/") {
            call.respondText("Hello, World!")
        }
    }

    CoroutineScope(Dispatchers.IO).launch {
        configureWebsocket(cobbledHuntersSession, authenticatedServers, ConfigHandler.config.secret)
    }
}

object CIOApplication {

    val gr = GlobalResources
    val gson = Gson()

    fun main() {
        gr.logger.info("Starting CIO server")
        embeddedServer(
            CIO,
            port = ConfigHandler.config.host.port,
            host = ConfigHandler.config.host.address,
            module = Application::module
        ).start(wait = true)

    }


    suspend fun sendToServers(message: SocketMessage) {
        cobbledHuntersSession.forEach {
            it.value.send(Frame.Text(gson.toJson(message)))
        }
    }
}