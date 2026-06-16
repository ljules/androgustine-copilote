package fr.augustine.androgustinecopilote.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.augustine.androgustinecopilote.data.CopilotFirestoreRepository
import fr.augustine.androgustinecopilote.data.CopilotFirestoreState
import fr.augustine.androgustinecopilote.data.FirestoreConnectionState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class CopilotUiState(
    val firestoreStatus: String = "Initialisation",
    val sessionId: String = "-",
    val status: String = "-",
    val raceStarted: String = "-",
    val currentLap: String = "-",
    val elapsedSessionS: String = "-",
    val elapsedLapS: String = "-",
    val activeStrategy: String = "-",
    val gpsSpeedKmh: String = "-",
    val gpsLat: String = "-",
    val gpsLon: String = "-",
    val snappedDistanceM: String = "-",
    val ghostDistanceM: String = "-",
    val deltaDistanceM: String = "-",
    val heartRateBpm: String = "-",
    val weatherTemperatureC: String = "-",
    val weatherWindKmh: String = "-",
    val weatherRainProbability: String = "-",
    val timestampIso: String = "-",
    val errorMessage: String? = null,
)

class CopilotViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = CopilotFirestoreRepository(application)

    val uiState: StateFlow<CopilotUiState> = repository.state
        .map(::toUiState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CopilotUiState(),
        )

    init {
        repository.start()
    }

    override fun onCleared() {
        repository.stop()
        super.onCleared()
    }

    private fun toUiState(state: CopilotFirestoreState): CopilotUiState {
        val session = state.session
        val telemetry = state.telemetry

        return CopilotUiState(
            firestoreStatus = state.connectionState.toDisplayText(),
            sessionId = session?.sessionId.orDash(),
            status = session?.status.orDash(),
            raceStarted = (telemetry?.raceStarted ?: session?.raceStarted).format(),
            currentLap = telemetry?.currentLap.format(),
            elapsedSessionS = telemetry?.elapsedSessionS.format(1),
            elapsedLapS = telemetry?.elapsedLapS.format(1),
            activeStrategy = telemetry?.activeStrategy.orDash(),
            gpsSpeedKmh = telemetry?.gpsSpeedKmh.format(1),
            gpsLat = telemetry?.gpsLat.format(6),
            gpsLon = telemetry?.gpsLon.format(6),
            snappedDistanceM = telemetry?.snappedDistanceM.format(1),
            ghostDistanceM = telemetry?.ghostDistanceM.format(1),
            deltaDistanceM = telemetry?.deltaDistanceM.format(1),
            heartRateBpm = telemetry?.heartRateBpm.format(),
            weatherTemperatureC = telemetry?.weatherTemperatureC.format(1),
            weatherWindKmh = telemetry?.weatherWindKmh.format(1),
            weatherRainProbability = telemetry?.weatherRainProbability.format(2),
            timestampIso = telemetry?.timestampIso.orDash(),
            errorMessage = state.errorMessage,
        )
    }

    private fun FirestoreConnectionState.toDisplayText(): String {
        return when (this) {
            FirestoreConnectionState.Initializing -> "Connexion Firestore..."
            FirestoreConnectionState.Connected -> "Connecte - telemetry/latest actif"
            FirestoreConnectionState.NoSession -> "Aucune session trouvee"
            FirestoreConnectionState.WaitingForTelemetry -> "Session trouvee - telemetry/latest absent"
            FirestoreConnectionState.Error -> "Erreur Firestore"
            FirestoreConnectionState.FirebaseNotInitialized -> "Firebase non initialise"
        }
    }
}

private fun String?.orDash(): String = this ?: "-"

private fun Boolean?.format(): String = when (this) {
    true -> "true"
    false -> "false"
    null -> "-"
}

private fun Long?.format(): String = this?.toString() ?: "-"

private fun Double?.format(decimals: Int): String {
    return this?.let { "%.${decimals}f".format(it) } ?: "-"
}
