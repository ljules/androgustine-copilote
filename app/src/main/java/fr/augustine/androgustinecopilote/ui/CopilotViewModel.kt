package fr.augustine.androgustinecopilote.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.augustine.androgustinecopilote.data.CopilotInstructions
import fr.augustine.androgustinecopilote.data.CopilotFirestoreRepository
import fr.augustine.androgustinecopilote.data.CopilotFirestoreState
import fr.augustine.androgustinecopilote.data.FirestoreConnectionState
import fr.augustine.androgustinecopilote.data.TrackData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.Locale

data class CopilotUiState(
    val firestoreStatus: String = "Initialisation",
    val hasSession: Boolean = false,
    val track: TrackData? = null,
    val snappedDistanceMRaw: Double? = null,
    val ghostDistanceMRaw: Double? = null,
    val trackName: String = "-",
    val sessionId: String = "-",
    val status: String = "-",
    val raceStarted: String = "-",
    val lapProgress: String = "- / ?",
    val sessionChrono: String = "--:--",
    val lapChrono: String = "--:--",
    val speedLabel: String = "- km/h",
    val heartRateLabel: String = "- bpm",
    val deltaGhostLabel: String = "- m",
    val weatherLabel: String = "-",
    val activeStrategy: String = "-",
    val currentLap: String = "-",
    val elapsedSessionS: String = "-",
    val elapsedLapS: String = "-",
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
    val selectedPaceInstruction: String = CopilotInstructions.PACE_MAINTAIN,
    val selectedRaceStatusInstruction: String = CopilotInstructions.STATUS_RACE,
    val pitStopRequest: Boolean = false,
    val lastInstructionSent: String = "-",
    val lastInstructionSentAt: String = "-",
    val instructionErrorMessage: String? = null,
    val isSendingInstruction: Boolean = false,
)

private data class InstructionSendUiState(
    val selectedPaceInstruction: String = CopilotInstructions.PACE_MAINTAIN,
    val selectedRaceStatusInstruction: String = CopilotInstructions.STATUS_RACE,
    val pitStopRequest: Boolean = false,
    val lastInstructionSent: String = "-",
    val lastInstructionSentAt: String = "-",
    val errorMessage: String? = null,
    val isSending: Boolean = false,
)

class CopilotViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = CopilotFirestoreRepository(application)
    private val instructionState = MutableStateFlow(InstructionSendUiState())

    val uiState: StateFlow<CopilotUiState> = repository.state
        .combine(instructionState, ::toUiState)
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

    fun sendPaceInstruction(paceInstruction: String) {
        val current = instructionState.value
        sendInstructions(
            paceInstruction = paceInstruction,
            raceStatusInstruction = current.selectedRaceStatusInstruction,
            pitStopRequest = current.pitStopRequest,
        )
    }

    fun sendRaceStatusInstruction(raceStatusInstruction: String) {
        val current = instructionState.value
        sendInstructions(
            paceInstruction = current.selectedPaceInstruction,
            raceStatusInstruction = raceStatusInstruction,
            pitStopRequest = current.pitStopRequest,
        )
    }

    fun sendPitStopRequest(pitStopRequest: Boolean) {
        val current = instructionState.value
        sendInstructions(
            paceInstruction = current.selectedPaceInstruction,
            raceStatusInstruction = current.selectedRaceStatusInstruction,
            pitStopRequest = pitStopRequest,
        )
    }

    private fun sendInstructions(
        paceInstruction: String,
        raceStatusInstruction: String,
        pitStopRequest: Boolean,
    ) {
        viewModelScope.launch {
            val updatedAtIso = Instant.now().toString()
            instructionState.update {
                it.copy(
                    selectedPaceInstruction = paceInstruction,
                    selectedRaceStatusInstruction = raceStatusInstruction,
                    pitStopRequest = pitStopRequest,
                    errorMessage = null,
                    isSending = true,
                )
            }

            val result = repository.sendInstructions(
                pilotPaceInstruction = paceInstruction,
                raceStatusInstruction = raceStatusInstruction,
                pitStopRequest = pitStopRequest,
                updatedAtIso = updatedAtIso,
            )

            instructionState.update {
                result.fold(
                    onSuccess = { instructions ->
                        it.copy(
                            lastInstructionSent = instructions.toDisplayText(),
                            lastInstructionSentAt = instructions.updatedAtIso,
                            errorMessage = null,
                            isSending = false,
                        )
                    },
                    onFailure = { exception ->
                        it.copy(
                            errorMessage = exception.message ?: "Erreur d'envoi de consigne.",
                            isSending = false,
                        )
                    },
                )
            }
        }
    }

    private fun toUiState(
        state: CopilotFirestoreState,
        instructionsState: InstructionSendUiState,
    ): CopilotUiState {
        val session = state.session
        val telemetry = state.telemetry

        return CopilotUiState(
            firestoreStatus = state.connectionState.toDisplayText(),
            hasSession = session != null,
            track = state.track,
            snappedDistanceMRaw = telemetry?.snappedDistanceM,
            ghostDistanceMRaw = telemetry?.ghostDistanceM,
            trackName = state.track?.trackName ?: session?.trackName.orDash(),
            sessionId = session?.sessionId.orDash(),
            status = session?.status.orDash(),
            raceStarted = (telemetry?.raceStarted ?: session?.raceStarted).format(),
            lapProgress = formatLapProgress(
                currentLap = telemetry?.currentLap,
                totalLaps = session?.totalLaps,
            ),
            sessionChrono = telemetry?.elapsedSessionS.formatChrono(),
            lapChrono = telemetry?.elapsedLapS.formatChrono(),
            speedLabel = "${telemetry?.gpsSpeedKmh.formatDecimal(1)} km/h",
            heartRateLabel = "${telemetry?.heartRateBpm.format()} bpm",
            deltaGhostLabel = "${telemetry?.deltaDistanceM.formatMeters()} m",
            weatherLabel = formatWeather(
                temperatureC = telemetry?.weatherTemperatureC,
                windKmh = telemetry?.weatherWindKmh,
                rainProbability = telemetry?.weatherRainProbability,
            ),
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
            selectedPaceInstruction = instructionsState.selectedPaceInstruction,
            selectedRaceStatusInstruction = instructionsState.selectedRaceStatusInstruction,
            pitStopRequest = instructionsState.pitStopRequest,
            lastInstructionSent = instructionsState.lastInstructionSent,
            lastInstructionSentAt = instructionsState.lastInstructionSentAt,
            instructionErrorMessage = instructionsState.errorMessage,
            isSendingInstruction = instructionsState.isSending,
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

private fun CopilotInstructions.toDisplayText(): String {
    return "$pilotPaceInstruction / $raceStatusInstruction / stand=$pitStopRequest"
}

private fun String?.orDash(): String = this ?: "-"

private fun Boolean?.format(): String = when (this) {
    true -> "true"
    false -> "false"
    null -> "-"
}

private fun Long?.format(): String = this?.toString() ?: "-"

private fun Double?.format(decimals: Int): String {
    return this?.let { String.format(Locale.FRANCE, "%.${decimals}f", it) } ?: "-"
}

private fun Double?.formatDecimal(decimals: Int): String {
    return this.format(decimals)
}

private fun Double?.formatChrono(): String {
    if (this == null) return "--:--"
    val totalSeconds = toLong().coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.FRANCE, "%02d:%02d", minutes, seconds)
}

private fun Double?.formatMeters(): String {
    return this?.let { String.format(Locale.FRANCE, "%.0f", it) } ?: "-"
}

private fun formatLapProgress(
    currentLap: Long?,
    totalLaps: Long?,
): String {
    val current = currentLap?.toString() ?: "-"
    val total = totalLaps?.toString() ?: "?"
    return "$current / $total"
}

private fun formatWeather(
    temperatureC: Double?,
    windKmh: Double?,
    rainProbability: Double?,
): String {
    return "${temperatureC.formatDecimal(1)} \u00B0C | vent ${windKmh.formatDecimal(1)} km/h | pluie ${rainProbability.formatRainPercent()}"
}

private fun Double?.formatRainPercent(): String {
    if (this == null) return "-"
    val percent = if (this in 0.0..1.0) this * 100 else this
    return "${String.format(Locale.FRANCE, "%.0f", percent)} %"
}
