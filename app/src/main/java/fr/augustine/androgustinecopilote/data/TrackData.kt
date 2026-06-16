package fr.augustine.androgustinecopilote.data

data class TrackPoint(
    val distanceM: Double,
    val lat: Double,
    val lon: Double,
)

data class TrackData(
    val trackName: String? = null,
    val totalDistanceM: Double? = null,
    val pointCount: Int? = null,
    val points: List<TrackPoint> = emptyList(),
)
