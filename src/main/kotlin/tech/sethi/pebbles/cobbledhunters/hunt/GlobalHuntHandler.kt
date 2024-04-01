package tech.sethi.pebbles.cobbledhunters.hunt

import com.google.gson.Gson
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import tech.sethi.pebbles.cobbledhunters.cio.CIOApplication
import tech.sethi.pebbles.cobbledhunters.cio.sockets.*
import tech.sethi.pebbles.cobbledhunters.db.DatabaseHandler
import tech.sethi.pebbles.cobbledhunters.hunt.type.*
import tech.sethi.pebbles.cobbledhunters.util.GlobalResources
import tech.sethi.pebbles.cobbledhunters.util.PM
import tech.sethi.pebbles.cobbledhunters.util.config.LangConfig
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object GlobalHuntHandler {
    val globalHuntPools: ConcurrentHashMap<String, GlobalHuntTracker?> = ConcurrentHashMap()
    val activeBossBars: ConcurrentHashMap<String, BossBar> = ConcurrentHashMap()


    val gson = Gson()

    var trackerUpdate = 0

    init {
        onLoad()
    }

    fun onLoad() {
        GlobalResources.logger.info("Loading Global Hunt Pools...")
        val huntPools = DatabaseHandler.globalHuntPoolCollection.find().toList()
        GlobalResources.logger.info("Loaded ${huntPools.size} Global Hunt Pools")

        huntPools.forEach { rollNewHunt(it.id) }

        CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                checkPoolsExpiration()

                activeBossBars.forEach { (_, bossBar) ->
                    val poolId = activeBossBars.entries.find { it.value == bossBar }?.key ?: return@forEach
                    val tracker = globalHuntPools[poolId] ?: return@forEach
                    if (tracker.expired().not()) {
                        val timeLeft = tracker.expireTime - System.currentTimeMillis()
                        val timeProgress = (timeLeft.toDouble() / (tracker.expireTime - tracker.rolledTime)).toFloat()
                        bossBar.progress(timeProgress)
                    }
                }

                trackerUpdate++

                if (trackerUpdate >= 8) {
                    globalHuntPools.forEach { (poolId, tracker) ->
                        val trackerUpdate = GlobalHuntTrackerUpdate(poolId, tracker)
                        val socketMessage =
                            SocketMessage(SocketMessageType.GLOBAL_HUNT_TRACKER_UPDATE, gson.toJson(trackerUpdate))

                        CoroutineScope(Dispatchers.IO).launch {
                            CIOApplication.sendToServers(socketMessage)
                        }
                    }
                    trackerUpdate = 0
                }

                Thread.sleep(1000)
            }
        }
    }

    fun reloadPools() {
        val huntPools = DatabaseHandler.globalHuntPoolCollection.find().toList()
        GlobalResources.logger.info("Loaded ${huntPools.size} Global Hunt Pools")

        // add new pools to the globalHuntPools
        huntPools.forEach { pool ->
            if (globalHuntPools.containsKey(pool.id).not()) {
                rollNewHunt(pool.id)
            }

            // remove pools that are no longer in the database
            globalHuntPools.keys.forEach { poolId ->
                if (huntPools.any { it.id == poolId }.not()) {
                    globalHuntPools.remove(poolId)
                    activeBossBars.remove(poolId)
                }
            }
        }

        // update the boss bars
        activeBossBars.forEach { (poolId, bossBar) ->
            val tracker = globalHuntPools[poolId] ?: return@forEach
            bossBar.name(PM.returnStyledText(createBossBarText(tracker)))
        }

        val poolsRefresh = GlobalHuntPoolsRefresh(globalHuntPools)
        val socketMessage = SocketMessage(SocketMessageType.GLOBA_HUNT_POOLS_REFRESH, gson.toJson(poolsRefresh))

        CoroutineScope(Dispatchers.IO).launch {
            CIOApplication.sendToServers(socketMessage)
        }
    }

    fun rollNewHunt(poolId: String) {
        val pool = DatabaseHandler.globalHuntPoolCollection.find().firstOrNull { it.id == poolId } ?: return
        val huntId = pool.huntIds.random()
        val hunt = DatabaseHandler.globalHuntCollection.find().firstOrNull { it.id == huntId }
        if (hunt == null) throw Exception("[CobbledHunters] Global Hunt in pool $poolId not found! Please check your config.")

        val expireTime = System.currentTimeMillis() + hunt.timeLimitMinutes * 60 * 1000
        val tracker = GlobalHuntTracker(
            hunt = hunt,
            rolledTime = System.currentTimeMillis(),
            expireTime = expireTime,
        )

        globalHuntPools[poolId] = tracker

        generateBossBar(poolId)
        PM.broadcast(LangConfig.config.globalPoolRefreshAnnouncement.replace("{pool}", pool.name))

        val trackerUpdate = GlobalHuntTrackerUpdate(poolId, tracker)
        val socketMessage = SocketMessage(SocketMessageType.GLOBAL_HUNT_TRACKER_UPDATE, gson.toJson(trackerUpdate))

        CoroutineScope(Dispatchers.IO).launch { CIOApplication.sendToServers(socketMessage) }
    }

    fun checkPoolsExpiration() {
        globalHuntPools.forEach { (poolId, tracker) ->
            if (tracker != null && tracker.expired()) expireHunt(poolId, tracker)
        }
    }

    fun joinHunt(player: Participant, poolId: String, balance: Double) {
        val canJoin = canJoinHunt(player, poolId, balance)

        if (canJoin.first.not()) {
            PM.audience(player.uuid)?.let { PM.sendMessage(it, canJoin.second) }
            return
        }

        val tracker = globalHuntPools[poolId] ?: return
        tracker.addParticipant(player)
        addPlayerToBossBar(player.uuid, poolId)

        PM.audience(player.uuid)?.let { PM.sendMessage(it, LangConfig.config.successfulJoin) }
    }

    fun canJoinHunt(participant: Participant, poolId: String, balance: Double): Pair<Boolean, String> {
        val tracker = globalHuntPools[poolId] ?: return Pair(false, LangConfig.config.huntNotFound)
        if (tracker.expired()) return Pair(false, LangConfig.config.huntExpired)
        if (tracker.isCompleted()) return Pair(false, LangConfig.config.huntAlreadyCompleted)
        if (tracker.isParticipant(participant.uuid)) return Pair(false, LangConfig.config.alreadyParticipating)
        if (tracker.hunt.cost > 0) {
            if (balance < tracker.hunt.cost) return Pair(false, LangConfig.config.notEnoughBalance)
        }

        return Pair(true, LangConfig.config.successfulJoin)
    }

    fun expireHunt(poolId: String, tracker: GlobalHuntTracker) {
        if (tracker.isCompleted().not()) {
            tracker.getParticipants().forEach {
                PM.audience(it.uuid)?.sendMessage(PM.returnStyledText(LangConfig.config.globalHuntExpired))
            }
        } else {
            tracker.getParticipants().forEach {
                PM.audience(it.uuid)?.sendMessage(PM.returnStyledText(LangConfig.config.globalHuntRefreshed))
            }
        }

        GlobalResources.server.allPlayers.forEach {
            activeBossBars[poolId]?.let { it1 -> it.hideBossBar(it1) }
        }
        activeBossBars.remove(poolId)

        rollNewHunt(poolId)
    }

    fun rewardHunters(poolId: String) {
        val tracker = globalHuntPools[poolId] ?: return
        if (tracker.rewarded) return

        tracker.rewarded = true

        val rewardPools = tracker.hunt.rewardPools
        val rolledRewardIds = rewardPools.map { it.reward }
        val rolledRewards = rolledRewardIds.map {
            DatabaseHandler.rewardCollection.find().firstOrNull { reward -> reward.id == it.rewardId }
        }
        val baseRewardIds = tracker.hunt.guaranteedRewardId
        val baseRewards =
            baseRewardIds.map { DatabaseHandler.rewardCollection.find().firstOrNull { reward -> reward.id == it } }

        val allRewards = rolledRewards + baseRewards

        CoroutineScope(Dispatchers.IO).launch {
            tracker.getParticipants().forEach { participant ->
                if (participant.progress > 0) {
                    for (reward in allRewards) {
                        if (reward == null) continue
                        val randomUuid = UUID.randomUUID().toString()
                        // add reward to the player's reward storage rewards map with a random uuid
                        DatabaseHandler.playerRewardStorageCollection.updateOne(
                            Filters.eq("playerUUID", participant.uuid),
                            Updates.push("rewards", RewardEntry(randomUuid, reward))
                        )
                    }

                    DatabaseHandler.playerRewardStorageCollection.updateOne(
                        Filters.eq("playerUUID", participant.uuid), Updates.inc("exp", tracker.hunt.experience)
                    )

                    val audience = PM.audience(participant.uuid)
                    audience?.sendMessage(PM.returnStyledText(LangConfig.config.rewardAdded))

                    CoroutineScope(Dispatchers.IO).launch {
                        val socketMessage = SocketMessage(
                            SocketMessageType.PLAY_SOUND, gson.toJson(
                                PlaySound(
                                    participant.uuid, "minecraft:ui.toast.challenge_complete", radius = 5.0
                                )
                            )
                        )
                        CIOApplication.sendToServers(socketMessage)
                    }
                } else {
                    val audience = PM.audience(participant.uuid)
                    audience?.sendMessage(PM.returnStyledText(LangConfig.config.noProgress))
                }
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            val ranking = tracker.getRanking()
            val rankingRewards = tracker.hunt.extraRankingRewards

            rankingRewards.forEachIndexed { index, rankingReward ->
                if (index >= ranking.size) return@forEachIndexed
                val participant = ranking[rankingReward.rank - 1]

                val rankRolledRewards = rankingReward.rewardPools.map {
                    DatabaseHandler.rewardCollection.find().firstOrNull { reward -> reward.id == it.reward.rewardId }
                }

                val rankBaseRewards = rankingReward.guaranteedRewardId.map {
                    DatabaseHandler.rewardCollection.find().firstOrNull { reward -> reward.id == it }
                }

                val allRankRewards = rankRolledRewards + rankBaseRewards

                DatabaseHandler.playerRewardStorageCollection.updateOne(
                    Filters.eq("playerUUID", participant.uuid),
                    Updates.pushEach("rewards", allRankRewards.filterNotNull().map { reward ->
                        RewardEntry(UUID.randomUUID().toString(), reward)
                    })
                )

                DatabaseHandler.playerRewardStorageCollection.updateOne(
                    Filters.eq("playerUUID", participant.uuid), Updates.inc("exp", rankingReward.experience)
                )

                val audience = PM.audience(participant.uuid)
                audience?.sendMessage(PM.returnStyledText(LangConfig.config.globalHuntRankingReward))
            }
        }
    }

    fun generateBossBar(poolId: String) {
        val tracker = globalHuntPools[poolId] ?: return
        val bossBar = BossBar.bossBar(
            PM.returnStyledText(createBossBarText(tracker)), 1.0f, BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS
        )

        activeBossBars[poolId] = bossBar
    }

    fun createBossBarText(tracker: GlobalHuntTracker) =
        "${tracker.hunt.name} <yellow>${tracker.getProgress()}/${tracker.hunt.amount}</yellow>"

    fun addPlayerToBossBar(playerUUID: String, poolId: String) {
        val bossBar = activeBossBars[poolId] ?: return
        PM.audience(playerUUID)?.let { bossBar.addViewer(it) }
    }

    fun onPokemonAction(participent: Participant, pokemon: PokemonFeature) {
        if (pokemon.goal != HuntGoals.CATCH && pokemon.wild.not()) return
        val tracker = globalHuntPools.values.firstOrNull { it?.isParticipant(participent.uuid) == true } ?: return
        val poolId = globalHuntPools.entries.firstOrNull { it.value == tracker }?.key ?: return
        val checkFeature = tracker.hunt.huntFeature.checkRequirement(pokemon, pokemon.goal)

        if (checkFeature.not()) return

        if (tracker.isCompleted().not()) {
            tracker.addProgress(participent.uuid, 1)
            val newBossBarName = createBossBarText(tracker)
            activeBossBars[poolId]?.name(PM.returnStyledText(newBossBarName))

            val trackerUpdate = GlobalHuntTrackerUpdate(poolId, tracker)
            val socketMessage = SocketMessage(SocketMessageType.GLOBAL_HUNT_TRACKER_UPDATE, gson.toJson(trackerUpdate))

            CoroutineScope(Dispatchers.IO).launch {
                CIOApplication.sendToServers(socketMessage)
            }
        }

        if (tracker.isCompleted() && tracker.success != true) {
            tracker.success = true

            rewardHunters(poolId)
            PM.broadcast(LangConfig.config.globalHuntCompletedBroadcast.replace("{hunt}", tracker.hunt.name))

            activeBossBars[poolId]?.let { bossBar ->
                GlobalResources.server.allPlayers.forEach {
                    it.hideBossBar(bossBar)
                }
            }
        }
    }

    fun participatingHunt(participant: Participant): GlobalHuntTracker? =
        globalHuntPools.values.firstOrNull { it?.isParticipant(participant.uuid) == true }


}