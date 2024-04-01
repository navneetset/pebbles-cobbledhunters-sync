package tech.sethi.pebbles.cobbledhunters.hunt.type

data class RankingRewards(
    val rank: Int, val guaranteedRewardId: List<String>, val rewardPools: List<Pool>, val experience: Int
)
