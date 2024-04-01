package tech.sethi.pebbles.cobbledhunters.util.config

import com.google.gson.GsonBuilder
import java.io.File

object ConfigHandler {
    val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    val configDir = "plugins/pebbles-cobbledhunters-sync"

    val configFile = File("$configDir/config.json")
    val mongoConfigFile = File("$configDir/mongodb.json")

    var config = Config()
    var mongoConfig = MongoDBConfig()


    init {
        reload()
    }

    fun reload() {
        if (configFile.exists()) {
            val configString = configFile.readText()
            config = gson.fromJson(configString, Config::class.java)
        } else {
            configFile.parentFile.mkdirs()
            configFile.createNewFile()
            val configString = gson.toJson(config)
            configFile.writeText(configString)
        }

        if (mongoConfigFile.exists()) {
            val mongoConfigString = mongoConfigFile.readText()
            mongoConfig = gson.fromJson(mongoConfigString, MongoDBConfig::class.java)
        } else {
            mongoConfigFile.parentFile.mkdirs()
            mongoConfigFile.createNewFile()
            val mongoConfigString = gson.toJson(mongoConfig)
            mongoConfigFile.writeText(mongoConfigString)
        }

        LangConfig.reload()
    }


    data class Config(
        val secret: String = "RANDOMSECRETHERE",
        val host: Host = Host(),
    )

    data class Host(
        val address: String = "0.0.0.0", val port: Int = 9999
    )

    data class MongoDBConfig(
        val connectionString: String = "mongodb://localhost:27017",
        val database: String = "pebbles_cobbledhunters",
        val globalHuntCollection: String = "GlobalHunts",
        val globalHuntPoolCollection: String = "GlobalHuntPools",
        val globalHuntSessionCollection: String = "GlobalHuntSessions",
        val personalHuntCollection: String = "PersonalHunts",
        val personalHuntPoolCollection: String = "PersonalHuntPools",
        val personalHuntSessionCollection: String = "PersonalHuntSessions",
        val rolledHuntTrackerCollection: String = "RolledHuntTrackers",
        val playerHuntCollection: String = "PlayerHunts",
        val rewardCollection: String = "Rewards",
        val rewardPoolCollection: String = "RewardPools",
        val playerRewardStorageCollection: String = "PlayerRewardStorage",
        val playerExpProgressCollection: String = "PlayerExpProgress"
    )

    data class SerializedItemStack(
        var displayName: String?,
        val material: String,
        val amount: Int,
        val nbt: String?,
        var lore: MutableList<String> = mutableListOf()
    ) {
        fun deepCopy(): SerializedItemStack {
            return SerializedItemStack(
                displayName, material, amount, nbt, lore.toMutableList()
            )
        }
    }
}