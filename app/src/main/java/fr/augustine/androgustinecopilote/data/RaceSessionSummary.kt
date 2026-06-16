package fr.augustine.androgustinecopilote.data

data class RaceSessionSummary(
    val sessionId: String,
    val createdAtIso: String? = null,
    val appRole: String? = null,
    val status: String? = null,
    val raceStarted: Boolean? = null,
    val totalLaps: Long? = null,
    val trackName: String? = null,
)
