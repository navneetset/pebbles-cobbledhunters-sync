package tech.sethi.pebbles.cobbledhunters.cio.sockets

import tech.sethi.pebbles.cobbledhunters.hunt.type.GlobalHuntTracker
import tech.sethi.pebbles.cobbledhunters.hunt.type.Participant
import tech.sethi.pebbles.cobbledhunters.hunt.type.PokemonFeature


data class SocketMessage(
    val type: SocketMessageType, val json: String
)

data class GlobalHuntJoinHunt(
    val poolId: String, val playerUUID: String, val playerName: String, val balance: Double
)

data class PokemonAction(
    val participant: Participant, val feature: PokemonFeature
)

data class GlobalHuntTrackerUpdate(
    val poolId: String, val tracker: GlobalHuntTracker?
)

data class GlobalHuntPoolsRefresh(
    val pools: MutableMap<String, GlobalHuntTracker?>
)

data class PlaySound(
    val uuid: String,
    val sound: String,
    val volume: Float = 1.0f,
    val pitch: Float = 1.0f,
    val radius: Double = 2.0
)