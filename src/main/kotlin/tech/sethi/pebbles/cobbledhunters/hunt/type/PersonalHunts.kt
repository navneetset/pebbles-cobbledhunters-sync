package tech.sethi.pebbles.cobbledhunters.hunt.type

data class PersonalHunts(
    val playerUUID: String,
    val playerName: String,
    var easyHunt: String?,
    var mediumHunt: String?,
    var hardHunt: String?,
    var legendaryHunt: String?,
    var godlikeHunt: String?
)
