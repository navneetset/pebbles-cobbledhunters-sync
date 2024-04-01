package tech.sethi.pebbles.cobbledhunters.db

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClients
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.configuration.CodecRegistries.fromProviders
import org.bson.codecs.configuration.CodecRegistry
import org.bson.codecs.kotlin.DataClassCodecProvider
import tech.sethi.pebbles.cobbledhunters.hunt.type.*
import tech.sethi.pebbles.cobbledhunters.util.config.ConfigHandler

object DatabaseHandler {
    val config = ConfigHandler.mongoConfig

    val mongoClientSettings =
        MongoClientSettings.builder().applyConnectionString(ConnectionString(config.connectionString))
            .codecRegistry(getCodecRegistry()).build()

    val mongoClient = MongoClients.create(mongoClientSettings)
    val database = mongoClient.getDatabase(config.database)

    val rewardCollection = database.getCollection(config.rewardCollection, HuntReward::class.java)

    val globalHuntCollection = database.getCollection(config.globalHuntCollection, GlobalHunt::class.java)
    val globalHuntPoolCollection = database.getCollection(config.globalHuntPoolCollection, HuntPool::class.java)

    val playerRewardStorageCollection =
        database.getCollection(config.playerRewardStorageCollection, RewardStorage::class.java)
    val playerExpProgressCollection =
        database.getCollection(config.playerExpProgressCollection, ExpProgress::class.java)

    fun getCodecRegistry(): CodecRegistry {
        return CodecRegistries.fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(), fromProviders(DataClassCodecProvider())
        )
    }

}