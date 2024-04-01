package tech.sethi.pebbles.cobbledhunters.hunt.type

data class HuntFeature(
    val species: List<String>? = listOf(),
    val type: PokemonTypes? = null,
    val levelRange: LevelRange = LevelRange(),
    val shiny: Boolean = false,
    val nature: PokemonNatures? = null,
    val gender: HuntGender = HuntGender.ANY,
    val ability: String? = null,
    val ball: HuntBalls? = HuntBalls.ANY,
    val form: String? = null,
    val goal: HuntGoals = HuntGoals.CATCH
) {
    fun checkRequirement(pokemon: PokemonFeature, goal: HuntGoals): Boolean {
        if (this.goal != goal) return false

        if (this.species != null && !this.species.contains(pokemon.species)) return false

        if (this.type != null && !pokemon.types.contains(this.type)) return false

        if (pokemon.level !in this.levelRange.min..this.levelRange.max) return false

        if (this.shiny && !pokemon.shiny) return false

        if (this.nature != null && this.nature != pokemon.nature) return false

        if (this.gender != HuntGender.ANY && pokemon.gender != this.gender) return false

        if (this.ability != null && this.ability != pokemon.ability) return false

        if (this.ball != HuntBalls.ANY && this.ball != pokemon.ball) return false

        if (this.form != null && this.form != pokemon.form) return false

        return true
    }
}