package tech.sethi.pebbles.cobbledhunters.hunt.type

import tech.sethi.pebbles.cobbledhunters.util.config.ConfigHandler

data class HuntReward(
    val id: String,
    var name: String,
    var amount: Int? = 1,
    val splitable: Boolean? = true,
    val displayItem: ConfigHandler.SerializedItemStack,
    var commands: List<String>,
    var message: String? = "You collected {display_item.name}",
) {
    fun deepCopy(): HuntReward {
        return HuntReward(
            id, name, amount, splitable, displayItem.deepCopy(), commands, message
        )
    }
}