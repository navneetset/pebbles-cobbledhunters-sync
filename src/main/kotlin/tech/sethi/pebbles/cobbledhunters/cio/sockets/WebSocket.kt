package tech.sethi.pebbles.cobbledhunters.cio.sockets

import com.google.gson.Gson
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import tech.sethi.pebbles.cobbledhunters.hunt.GlobalHuntHandler
import tech.sethi.pebbles.cobbledhunters.hunt.type.Participant
import tech.sethi.pebbles.cobbledhunters.util.GlobalResources
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

fun Application.configureWebsocket(
    cobbledHuntersSession: ConcurrentHashMap<String, WebSocketSession>, authenticatedServers: MutableSet<String>, secret: String
) {
    val gson = Gson()

    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(30)
        timeout = Duration.ofSeconds(30)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        webSocket("/cobbled-hunters") {
            var isAuthenticated =
                authenticatedServers.contains("${this.call.request.local.remoteHost}:${this.call.request.local.remotePort}")

            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()

                    if (!isAuthenticated) {
                        if (text == secret) {
                            isAuthenticated = true
                            authenticatedServers.add("${this.call.request.local.remoteHost}:${this.call.request.local.remotePort}")
                            cobbledHuntersSession["${this.call.request.local.remoteHost}:${this.call.request.local.remotePort}"] = this
                            GlobalResources.logger.info("Server ${this.call.request.local.remoteHost}:${this.call.request.local.remotePort} authenticated Cobbled Hunters socket!")
                            continue
                        } else {
                            close(CloseReason(CloseReason.Codes.NORMAL, "Failed to connect"))
                            return@webSocket
                        }
                    }

                    if (text != secret) {
                        val message = try {
                            gson.fromJson(text, SocketMessage::class.java)
                        } catch (e: Exception) {
                            GlobalResources.logger.error("Failed to parse message: $text")
                            continue
                        }

                        when (message.type) {
                            SocketMessageType.GLOBAL_HUNT_JOIN_HUNT -> {
                                val joinHunt = gson.fromJson(message.json, GlobalHuntJoinHunt::class.java)
                                val participant = Participant(joinHunt.playerUUID, joinHunt.playerName)
                                GlobalHuntHandler.joinHunt(participant, joinHunt.poolId, joinHunt.balance)
                            }

                            SocketMessageType.GLOBAL_HUNT_POKEMON_ACTION -> {
                                val pokemonAction = gson.fromJson(message.json, PokemonAction::class.java)
                                GlobalHuntHandler.onPokemonAction(pokemonAction.participant, pokemonAction.feature)
                            }

                            else -> {
                                // Do nothing
                            }
                        }
                    }

                    if (text.equals("bye", ignoreCase = true)) {
                        close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                    }
                }
            }
        }
    }
}

