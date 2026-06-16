package fr.augustine.androgustinecopilote.data

data class CopilotTelemetrySnapshot(
    val timestampIso: String? = null,
    val elapsedSessionS: Double? = null,
    val elapsedLapS: Double? = null,
    val currentLap: Long? = null,
    val activeStrategy: String? = null,
    val gpsLat: Double? = null,
    val gpsLon: Double? = null,
    val gpsSpeedKmh: Double? = null,
    val snappedDistanceM: Double? = null,
    val ghostDistanceM: Double? = null,
    val deltaDistanceM: Double? = null,
    val heartRateBpm: Long? = null,
    val weatherTemperatureC: Double? = null,
    val weatherWindKmh: Double? = null,
    val weatherRainProbability: Double? = null,
    val raceStarted: Boolean? = null,
)
