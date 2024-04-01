package tech.sethi.pebbles.cobbledhunters.util

import com.velocitypowered.api.proxy.Player
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import java.util.*
import kotlin.jvm.optionals.getOrNull

object PM {

    fun returnStyledText(message: String, style: Boolean? = true): Component {
        val mmFull = MiniMessage.miniMessage().deserialize(message).decoration(TextDecoration.ITALIC, false)
        val mmNoStyle = MiniMessage.builder().tags(TagResolver.empty()).build().deserialize(message)
            .decoration(TextDecoration.ITALIC, false)

        return if (style == true) {
            mmFull
        } else {
            mmNoStyle
        }
    }


    fun broadcast(message: String) {
        GlobalResources.server.allPlayers.forEach {
            it.sendMessage(returnStyledText(message))
        }
    }

    fun sendMessage(player: Player, message: String) {
        player.sendMessage(returnStyledText(message))
    }

    fun getPlayer(uuidOrName: String): Player? {
        return try {
            GlobalResources.server.getPlayer(UUID.fromString(uuidOrName)).getOrNull()
        } catch (e: IllegalArgumentException) {
            GlobalResources.server.getPlayer(uuidOrName).getOrNull()
        }
    }

    fun audience(uuidOrName: String): Player? {
        return try {
            GlobalResources.server.getPlayer(UUID.fromString(uuidOrName)).getOrNull()
        } catch (e: IllegalArgumentException) {
            GlobalResources.server.getPlayer(uuidOrName).getOrNull()
        }
    }
}