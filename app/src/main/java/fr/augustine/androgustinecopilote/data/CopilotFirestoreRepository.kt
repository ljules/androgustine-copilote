package fr.augustine.androgustinecopilote.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class FirestoreConnectionState {
    Initializing,
    Connected,
    NoSession,
    WaitingForTelemetry,
    Error,
    FirebaseNotInitialized,
}

data class CopilotFirestoreState(
    val session: RaceSessionSummary? = null,
    val telemetry: CopilotTelemetrySnapshot? = null,
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
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    clearTelemetryListener()
                    _state.value = CopilotFirestoreState(
                        connectionState = FirestoreConnectionState.Error,
                        errorMessage = error.message,
                    )
                    return@addSnapshotListener
                }

                val latestSessionDocument = snapshots?.documents?.firstOrNull()
                if (latestSessionDocument == null) {
                    clearTelemetryListener()
                    observedSessionId = null
                    _state.value = CopilotFirestoreState(connectionState = FirestoreConnectionState.NoSession)
                    return@addSnapshotListener
                }

                val session = latestSessionDocument.toRaceSessionSummary()
                _state.update {
                    it.copy(
                        session = session,
                        telemetry = if (observedSessionId == session.sessionId) it.telemetry else null,
                        connectionState = FirestoreConnectionState.WaitingForTelemetry,
                        errorMessage = null,
                    )
                }

                if (observedSessionId != session.sessionId) {
                    observedSessionId = session.sessionId
                    listenToTelemetry(firestore, session.sessionId)
                }
            }
    }

    fun stop() {
        sessionListener?.remove()
        sessionListener = null
        clearTelemetryListener()
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

                if (snapshot == null || !snapshot.exists()) {
                    _state.update {
                        it.copy(
                            telemetry = null,
                            connectionState = FirestoreConnectionState.WaitingForTelemetry,
                            errorMessage = null,
                        )
                    }
                    return@addSnapshotListener
                }

                _state.update {
                    it.copy(
                        telemetry = snapshot.toCopilotTelemetrySnapshot(),
                        connectionState = FirestoreConnectionState.Connected,
                        errorMessage = null,
                    )
                }
            }
    }

    private fun clearTelemetryListener() {
        telemetryListener?.remove()
        telemetryListener = null
    }

    private fun DocumentSnapshot.toRaceSessionSummary(): RaceSessionSummary {
        return RaceSessionSummary(
            sessionId = getString(FIELD_SESSION_ID) ?: id,
            createdAtIso = getString(FIELD_CREATED_AT_ISO),
            appRole = getString(FIELD_APP_ROLE),
            status = getString(FIELD_STATUS),
            raceStarted = getBoolean(FIELD_RACE_STARTED),
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

    private fun DocumentSnapshot.getNumberField(field: String): Number? {
        return get(field) as? Number
    }

    companion object {
        private const val RACE_SESSIONS_COLLECTION = "raceSessions"
        private const val TELEMETRY_COLLECTION = "telemetry"
        private const val LATEST_TELEMETRY_DOCUMENT = "latest"

        private const val FIELD_SESSION_ID = "sessionId"
        private const val FIELD_CREATED_AT_ISO = "createdAtIso"
        private const val FIELD_APP_ROLE = "appRole"
        private const val FIELD_STATUS = "status"
        private const val FIELD_RACE_STARTED = "raceStarted"
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
