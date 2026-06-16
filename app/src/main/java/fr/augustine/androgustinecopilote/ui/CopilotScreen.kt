package fr.augustine.androgustinecopilote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.augustine.androgustinecopilote.ui.theme.AndroGustineCopiloteTheme

@Composable
fun CopilotRoute(
    viewModel: CopilotViewModel = viewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    CopilotScreen(uiState = uiState.value)
}

@Composable
fun CopilotScreen(
    uiState: CopilotUiState,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "AndroGustine Copilote",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = uiState.firestoreStatus,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                uiState.errorMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                TelemetryRow("sessionId", uiState.sessionId)
                TelemetryRow("status", uiState.status)
                TelemetryRow("raceStarted", uiState.raceStarted)
                HorizontalDivider()
                TelemetryRow("currentLap", uiState.currentLap)
                TelemetryRow("elapsedSessionS", uiState.elapsedSessionS)
                TelemetryRow("elapsedLapS", uiState.elapsedLapS)
                TelemetryRow("activeStrategy", uiState.activeStrategy)
                TelemetryRow("gpsSpeedKmh", uiState.gpsSpeedKmh)
                TelemetryRow("gpsLat", uiState.gpsLat)
                TelemetryRow("gpsLon", uiState.gpsLon)
                TelemetryRow("snappedDistanceM", uiState.snappedDistanceM)
                TelemetryRow("ghostDistanceM", uiState.ghostDistanceM)
                TelemetryRow("deltaDistanceM", uiState.deltaDistanceM)
                TelemetryRow("heartRateBpm", uiState.heartRateBpm)
                TelemetryRow("weatherTemperatureC", uiState.weatherTemperatureC)
                TelemetryRow("weatherWindKmh", uiState.weatherWindKmh)
                TelemetryRow("weatherRainProbability", uiState.weatherRainProbability)
                TelemetryRow("timestampIso", uiState.timestampIso)
            }
        }
    }
}

@Composable
private fun TelemetryRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CopilotScreenPreview() {
    AndroGustineCopiloteTheme {
        CopilotScreen(
            uiState = CopilotUiState(
                firestoreStatus = "Connecte - telemetry/latest actif",
                sessionId = "demo-session",
                status = "running",
                raceStarted = "true",
                currentLap = "3",
                gpsSpeedKmh = "31.4",
                timestampIso = "2026-06-16T14:00:00Z",
            ),
        )
    }
}
