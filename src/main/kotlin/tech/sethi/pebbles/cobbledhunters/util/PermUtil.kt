package tech.sethi.pebbles.cobbledhunters.util

import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.server.RegisteredServer
import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider

object PermUtil {
    val luckpermProvider: LuckPerms? by lazy {
        getLuckPermsApi()
    }

    private fun isLuckPermsPresent(): Boolean {
        return try {
            Class.forName("net.luckperms.api.LuckPerms")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    private fun getLuckPermsApi(): LuckPerms? {
        return try {
            LuckPermsProvider.get()
        } catch (e: IllegalStateException) {
            null
        }
    }

    fun commandRequiresPermission(source: Player, permission: String): Boolean {
        val playerUuid = source.gameProfile.id
        return playerUuid != null && (source.hasPermission(permission) || isLuckPermsPresent() && getLuckPermsApi()?.userManager?.getUser(
            playerUuid
        )!!.cachedData.permissionData.checkPermission(permission).asBoolean()) || source.gameProfile == null
    }

    fun commandSourceRequiresPermission(source: CommandSource, permission: String): Boolean {
        return source.hasPermission(permission) || source is RegisteredServer
    }

    fun fetchPlayerPrefix(player: Player): String? {
        return luckpermProvider?.userManager?.getUser(player.gameProfile.id!!)
            ?.cachedData
            ?.getMetaData()
            ?.prefix
    }

    fun fetchPlayerSuffix(player: Player): String? {
        return luckpermProvider?.userManager?.getUser(player.gameProfile.id!!)
            ?.cachedData
            ?.getMetaData()
            ?.suffix
    }
}