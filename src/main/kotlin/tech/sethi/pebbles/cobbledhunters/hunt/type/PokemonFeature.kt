package tech.sethi.pebbles.cobbledhunters.hunt.type

data class PokemonFeature(
    val species: String,
    val types: List<PokemonTypes>,
    val level: Int,
    val shiny: Boolean,
    val nature: PokemonNatures,
    val gender: HuntGender,
    val ability: String,
    val ball: HuntBalls?,
    val form: String,
    val goal: HuntGoals,
    val wild: Boolean
)