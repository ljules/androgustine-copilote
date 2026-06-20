package fr.augustine.androgustinecopilote.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

enum class FirestoreConnectionState {
    Initializing,
    Connected,
    OfflineCache,
    NoSession,
    WaitingForTelemetry,
    Error,
    FirebaseNotInitialized,
}

data class CopilotFirestoreState(
    val session: RaceSessionSummary? = null,
    val telemetry: CopilotTelemetrySnapshot? = null,
    val track: TrackData? = null,
    val strategy: StrategyData? = null,
    val connectionState: FirestoreConnectionState = FirestoreConnectionState.Initializing,
    val errorMessage: String? = null,
)

class CopilotFirestoreRepository(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val _state = MutableStateFlow(CopilotFirestoreState())
    val state: StateFlow<CopilotFirestoreState> = _state.asStateFlow()

    private var sessionListener: ListenerRegistration? = null
    private var telemetryListener: ListenerRegistration? = null
    private var trackListener: ListenerRegistration? = null
    private var strategyListener: ListenerRegistration? = null
    private var observedSessionId: String? = null

    fun start() {
        stop()

        if (FirebaseApp.getApps(appContext).isEmpty()) {
            _state.value = CopilotFirestoreState(
                connectionState = FirestoreConnectionState.FirebaseNotInitialized,
                errorMessage = "Firebase n'est pas initialise. Verifiez app/google-services.json.",
            )
            return
        }

        val firestore = try {
            FirebaseFirestore.getInstance()
        } catch (exception: IllegalStateException) {
            _state.value = CopilotFirestoreState(
                connectionState = FirestoreConnectionState.FirebaseNotInitialized,
                errorMessage = exception.message,
            )
            return
        }

        _state.value = CopilotFirestoreState(connectionState = FirestoreConnectionState.Initializing)
        sessionListener = firestore.collection(RACE_SESSIONS_COLLECTION)
            .orderBy(FIELD_CREATED_AT_ISO, Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshots, error ->
                if (error != null) {
                    clearTelemetryListener()
                    clearTrackListener()
                    clearStrategyListener()
                    _state.value = CopilotFirestoreState(
                        connectionState = FirestoreConnectionState.Error,
                        errorMessage = error.message,
                    )
                    return@addSnapshotListener
                }

                val latestSessionDocument = snapshots?.documents?.firstOrNull()
                if (latestSessionDocument == null) {
                    clearTelemetryListener()
                    clearTrackListener()
                    clearStrategyListener()
                    observedSessionId = null
                    _state.value = CopilotFirestoreState(
                        connectionState = if (snapshots?.metadata?.isFromCache == true) {
                            FirestoreConnectionState.OfflineCache
                        } else {
                            FirestoreConnectionState.NoSession
                        },
                    )
                    return@addSnapshotListener
                }

                val session = latestSessionDocument.toRaceSessionSummary()
                val sessionConnectionState = if (snapshots.metadata.isFromCache) {
                    FirestoreConnectionState.OfflineCache
                } else {
                    FirestoreConnectionState.WaitingForTelemetry
                }
                _state.update {
                    it.copy(
                        session = session,
                        telemetry = if (observedSessionId == session.sessionId) it.telemetry else null,
                        track = if (observedSessionId == session.sessionId) it.track else null,
                        strategy = if (observedSessionId == session.sessionId) it.strategy else null,
                        connectionState = sessionConnectionState,
                        errorMessage = null,
                    )
                }

                if (observedSessionId != session.sessionId) {
                    observedSessionId = session.sessionId
                    listenToTelemetry(firestore, session.sessionId)
                    listenToTrack(firestore, session.sessionId)
                    listenToStrategy(firestore, session.sessionId)
                }
            }
    }

    fun stop() {
        sessionListener?.remove()
        sessionListener = null
        clearTelemetryListener()
        clearTrackListener()
        clearStrategyListener()
    }

    suspend fun sendInstructions(
        pilotPaceInstruction: String,
        raceStatusInstruction: String,
        pitStopRequest: Boolean,
        updatedAtIso: String,
    ): Result<CopilotInstructions> {
        val sessionId = _state.value.session?.sessionId
            ?: return Result.failure(IllegalStateException("Aucune session courante detectee."))
        val firestore = try {
            FirebaseFirestore.getInstance()
        } catch (exception: IllegalStateException) {
            return Result.failure(exception)
        }
        val instructions = CopilotInstructions(
            pilotPaceInstruction = pilotPaceInstruction,
            raceStatusInstruction = raceStatusInstruction,
            pitStopRequest = pitStopRequest,
            updatedAtIso = updatedAtIso,
        )

        return suspendCancellableCoroutine { continuation ->
            firestore.collection(RACE_SESSIONS_COLLECTION)
                .document(sessionId)
                .collection(INSTRUCTIONS_COLLECTION)
                .document(CURRENT_INSTRUCTIONS_DOCUMENT)
                .set(instructions.toFirestoreMap())
                .addOnSuccessListener {
                    if (continuation.isActive) {
                        continuation.resume(Result.success(instructions))
                    }
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(exception))
                    }
                }
        }
    }

    private fun listenToTelemetry(
        firestore: FirebaseFirestore,
        sessionId: String,
    ) {
        clearTelemetryListener()
        telemetryListener = firestore.collection(RACE_SESSIONS_COLLECTION)
            .document(sessionId)
            .collection(TELEMETRY_COLLECTION)
            .document(LATEST_TELEMETRY_DOCUMENT)
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    _state.update {
                        it.copy(
                            connectionState = FirestoreConnectionState.Error,
                            errorMessage = error.message,
                        )
                    }
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) {
                    _state.update {
                        it.copy(
                            telemetry = null,
                            connectionState = if (snapshot?.metadata?.isFromCache == true) {
                                FirestoreConnectionState.OfflineCache
                            } else {
                                FirestoreConnectionState.WaitingForTelemetry
                            },
                            errorMessage = null,
                        )
                    }
                    return@addSnapshotListener
                }

                _state.update {
                    it.copy(
                        telemetry = snapshot.toCopilotTelemetrySnapshot(),
                        connectionState = if (snapshot.metadata.isFromCache) {
                            FirestoreConnectionState.OfflineCache
                        } else {
                            FirestoreConnectionState.Connected
                        },
                        errorMessage = null,
                    )
                }
            }
    }

    private fun listenToStrategy(
        firestore: FirebaseFirestore,
        sessionId: String,
    ) {
        clearStrategyListener()
        strategyListener = firestore.collection(RACE_SESSIONS_COLLECTION)
            .document(sessionId)
            .collection(STRATEGY_COLLECTION)
            .document(CURRENT_STRATEGY_DOCUMENT)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _state.update {
                        it.copy(
                            connectionState = FirestoreConnectionState.Error,
                            errorMessage = error.message,
                        )
                    }
                    return@addSnapshotListener
                }

                _state.update {
                    it.copy(
                        strategy = if (snapshot != null && snapshot.exists()) {
                            snapshot.toStrategyData()
                        } else {
                            null
                        },
                        errorMessage = null,
                    )
                }
            }
    }

    private fun listenToTrack(
        firestore: FirebaseFirestore,
        sessionId: String,
    ) {
        clearTrackListener()
        trackListener = firestore.collection(RACE_SESSIONS_COLLECTION)
            .document(sessionId)
            .collection(TRACK_COLLECTION)
            .document(CURRENT_TRACK_DOCUMENT)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _state.update {
                        it.copy(
                            connectionState = FirestoreConnectionState.Error,
                            errorMessage = error.message,
                        )
                    }
                    return@addSnapshotListener
                }

                _state.update {
                    it.copy(
                        track = if (snapshot != null && snapshot.exists()) {
                            snapshot.toTrackData()
                        } else {
                            null
                        },
                        errorMessage = null,
                    )
                }
            }
    }

    private fun clearTelemetryListener() {
        telemetryListener?.remove()
        telemetryListener = null
    }

    private fun clearTrackListener() {
        trackListener?.remove()
        trackListener = null
    }

    private fun clearStrategyListener() {
        strategyListener?.remove()
        strategyListener = null
    }

    private fun DocumentSnapshot.toRaceSessionSummary(): RaceSessionSummary {
        return RaceSessionSummary(
            sessionId = getString(FIELD_SESSION_ID) ?: id,
            createdAtIso = getString(FIELD_CREATED_AT_ISO),
            appRole = getString(FIELD_APP_ROLE),
            status = getString(FIELD_STATUS),
            raceStarted = getBoolean(FIELD_RACE_STARTED),
            totalLaps = getNumberField(FIELD_TOTAL_LAPS)?.toLong(),
            trackName = getString(FIELD_TRACK_NAME),
        )
    }

    private fun DocumentSnapshot.toCopilotTelemetrySnapshot(): CopilotTelemetrySnapshot {
        return CopilotTelemetrySnapshot(
            timestampIso = getString(FIELD_TIMESTAMP_ISO),
            elapsedSessionS = getNumberField(FIELD_ELAPSED_SESSION_S)?.toDouble(),
            elapsedLapS = getNumberField(FIELD_ELAPSED_LAP_S)?.toDouble(),
            currentLap = getNumberField(FIELD_CURRENT_LAP)?.toLong(),
            activeStrategy = getString(FIELD_ACTIVE_STRATEGY),
            gpsLat = getNumberField(FIELD_GPS_LAT)?.toDouble(),
            gpsLon = getNumberField(FIELD_GPS_LON)?.toDouble(),
            gpsSpeedKmh = getNumberField(FIELD_GPS_SPEED_KMH)?.toDouble(),
            snappedDistanceM = getNumberField(FIELD_SNAPPED_DISTANCE_M)?.toDouble(),
            ghostDistanceM = getNumberField(FIELD_GHOST_DISTANCE_M)?.toDouble(),
            deltaDistanceM = getNumberField(FIELD_DELTA_DISTANCE_M)?.toDouble(),
            heartRateBpm = getNumberField(FIELD_HEART_RATE_BPM)?.toLong(),
            weatherTemperatureC = getNumberField(FIELD_WEATHER_TEMPERATURE_C)?.toDouble(),
            weatherWindKmh = getNumberField(FIELD_WEATHER_WIND_KMH)?.toDouble(),
            weatherRainProbability = getNumberField(FIELD_WEATHER_RAIN_PROBABILITY)?.toDouble(),
            raceStarted = getBoolean(FIELD_RACE_STARTED),
        )
    }

    private fun DocumentSnapshot.toTrackData(): TrackData {
        return TrackData(
            trackName = getString(FIELD_TRACK_NAME),
            totalDistanceM = getNumberField(FIELD_TOTAL_DISTANCE_M)?.toDouble(),
            pointCount = getNumberField(FIELD_POINT_COUNT)?.toInt(),
            points = getTrackPoints(),
        )
    }

    private fun DocumentSnapshot.getTrackPoints(): List<TrackPoint> {
        val rawPoints = get(FIELD_POINTS) as? List<*> ?: return emptyList()
        return rawPoints.mapNotNull { rawPoint ->
            val point = rawPoint as? Map<*, *> ?: return@mapNotNull null
            val distanceM = point[FIELD_DISTANCE_M] as? Number ?: return@mapNotNull null
            val lat = point[FIELD_LAT] as? Number ?: return@mapNotNull null
            val lon = point[FIELD_LON] as? Number ?: return@mapNotNull null
            TrackPoint(
                distanceM = distanceM.toDouble(),
                lat = lat.toDouble(),
                lon = lon.toDouble(),
            )
        }
    }

    private fun DocumentSnapshot.toStrategyData(): StrategyData {
        return StrategyData(
            startSegments = getStrategySegments(FIELD_START_SEGMENTS),
            raceSegments = getStrategySegments(FIELD_RACE_SEGMENTS),
        )
    }

    private fun DocumentSnapshot.getStrategySegments(field: String): List<StrategySegment> {
        val rawSegments = get(field) as? List<*> ?: return emptyList()
        return rawSegments.mapNotNull { rawSegment ->
            val segment = rawSegment as? Map<*, *> ?: return@mapNotNull null
            val startDistanceM = segment.firstNumber(
                FIELD_START_DISTANCE_M,
                FIELD_FROM_DISTANCE_M,
                FIELD_DISTANCE_START_M,
                FIELD_BEGIN_DISTANCE_M,
            ) ?: return@mapNotNull null
            val endDistanceM = segment.firstNumber(
                FIELD_END_DISTANCE_M,
                FIELD_TO_DISTANCE_M,
                FIELD_DISTANCE_END_M,
                FIELD_FINISH_DISTANCE_M,
            ) ?: return@mapNotNull null

            StrategySegment(
                startDistanceM = startDistanceM.toDouble(),
                endDistanceM = endDistanceM.toDouble(),
                colorKey = segment.firstString(
                    FIELD_COLOR,
                    FIELD_SEGMENT_COLOR,
                    FIELD_COLOR_KEY,
                    FIELD_KIND,
                    FIELD_TYPE,
                ),
                label = segment.firstString(FIELD_LABEL, FIELD_NAME),
            )
        }
    }

    private fun Map<*, *>.firstNumber(vararg fields: String): Number? {
        return fields.firstNotNullOfOrNull { field -> this[field] as? Number }
    }

    private fun Map<*, *>.firstString(vararg fields: String): String? {
        return fields.firstNotNullOfOrNull { field -> this[field] as? String }
    }

    private fun DocumentSnapshot.getNumberField(field: String): Number? {
        return get(field) as? Number
    }

    companion object {
        private const val RACE_SESSIONS_COLLECTION = "raceSessions"
        private const val TELEMETRY_COLLECTION = "telemetry"
        private const val LATEST_TELEMETRY_DOCUMENT = "latest"
        private const val TRACK_COLLECTION = "track"
        private const val CURRENT_TRACK_DOCUMENT = "current"
        private const val STRATEGY_COLLECTION = "strategy"
        private const val CURRENT_STRATEGY_DOCUMENT = "current"
        private const val INSTRUCTIONS_COLLECTION = "instructions"
        private const val CURRENT_INSTRUCTIONS_DOCUMENT = "current"

        private const val FIELD_SESSION_ID = "sessionId"
        private const val FIELD_CREATED_AT_ISO = "createdAtIso"
        private const val FIELD_APP_ROLE = "appRole"
        private const val FIELD_STATUS = "status"
        private const val FIELD_RACE_STARTED = "raceStarted"
        private const val FIELD_TOTAL_LAPS = "totalLaps"
        private const val FIELD_TRACK_NAME = "trackName"
        private const val FIELD_TOTAL_DISTANCE_M = "totalDistanceM"
        private const val FIELD_POINT_COUNT = "pointCount"
        private const val FIELD_POINTS = "points"
        private const val FIELD_DISTANCE_M = "distanceM"
        private const val FIELD_LAT = "lat"
        private const val FIELD_LON = "lon"
        private const val FIELD_START_SEGMENTS = "startSegments"
        private const val FIELD_RACE_SEGMENTS = "raceSegments"
        private const val FIELD_START_DISTANCE_M = "startDistanceM"
        private const val FIELD_END_DISTANCE_M = "endDistanceM"
        private const val FIELD_FROM_DISTANCE_M = "fromDistanceM"
        private const val FIELD_TO_DISTANCE_M = "toDistanceM"
        private const val FIELD_DISTANCE_START_M = "distanceStartM"
        private const val FIELD_DISTANCE_END_M = "distanceEndM"
        private const val FIELD_BEGIN_DISTANCE_M = "beginDistanceM"
        private const val FIELD_FINISH_DISTANCE_M = "finishDistanceM"
        private const val FIELD_COLOR = "color"
        private const val FIELD_SEGMENT_COLOR = "segmentColor"
        private const val FIELD_COLOR_KEY = "colorKey"
        private const val FIELD_KIND = "kind"
        private const val FIELD_TYPE = "type"
        private const val FIELD_LABEL = "label"
        private const val FIELD_NAME = "name"
        private const val FIELD_TIMESTAMP_ISO = "timestampIso"
        private const val FIELD_ELAPSED_SESSION_S = "elapsedSessionS"
        private const val FIELD_ELAPSED_LAP_S = "elapsedLapS"
        private const val FIELD_CURRENT_LAP = "currentLap"
        private const val FIELD_ACTIVE_STRATEGY = "activeStrategy"
        private const val FIELD_GPS_LAT = "gpsLat"
        private const val FIELD_GPS_LON = "gpsLon"
        private const val FIELD_GPS_SPEED_KMH = "gpsSpeedKmh"
        private const val FIELD_SNAPPED_DISTANCE_M = "snappedDistanceM"
        private const val FIELD_GHOST_DISTANCE_M = "ghostDistanceM"
        private const val FIELD_DELTA_DISTANCE_M = "deltaDistanceM"
        private const val FIELD_HEART_RATE_BPM = "heartRateBpm"
        private const val FIELD_WEATHER_TEMPERATURE_C = "weatherTemperatureC"
        private const val FIELD_WEATHER_WIND_KMH = "weatherWindKmh"
        private const val FIELD_WEATHER_RAIN_PROBABILITY = "weatherRainProbability"
    }
}
