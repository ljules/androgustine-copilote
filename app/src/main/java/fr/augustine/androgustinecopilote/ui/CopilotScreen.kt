package fr.augustine.androgustinecopilote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DashboardHeader(
                    firestoreStatus = uiState.firestoreStatus,
                    errorMessage = uiState.errorMessage,
                )
                SessionCard(
                    trackName = uiState.trackName,
                    sessionId = uiState.sessionId,
                    status = uiState.status,
                )
                PrimaryMetricsGrid(
                    lapProgress = uiState.lapProgress,
                    sessionChrono = uiState.sessionChrono,
                    lapChrono = uiState.lapChrono,
                    speedLabel = uiState.speedLabel,
                    deltaGhostLabel = uiState.deltaGhostLabel,
                    heartRateLabel = uiState.heartRateLabel,
                )
                SecondaryCard(
                    title = "Meteo",
                    value = uiState.weatherLabel,
                )
                DebugSection(uiState = uiState)
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    firestoreStatus: String,
    errorMessage: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "AndroGustine Copilote",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = firestoreStatus,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        errorMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun SessionCard(
    trackName: String,
    sessionId: String,
    status: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Circuit : $trackName",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Session : $sessionId",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = "Etat : $status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun PrimaryMetricsGrid(
    lapProgress: String,
    sessionChrono: String,
    lapChrono: String,
    speedLabel: String,
    deltaGhostLabel: String,
    heartRateLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetricCard(
                title = "Tour",
                value = lapProgress,
                modifier = Modifier.weight(1f),
                prominent = true,
            )
            MetricCard(
                title = "Chrono",
                value = sessionChrono,
                modifier = Modifier.weight(1f),
                prominent = true,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetricCard(
                title = "Temps tour",
                value = lapChrono,
                modifier = Modifier.weight(1f),
                prominent = true,
            )
            MetricCard(
                title = "Vitesse",
                value = speedLabel,
                modifier = Modifier.weight(1f),
                prominent = true,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetricCard(
                title = "Delta Ghost",
                value = deltaGhostLabel,
                modifier = Modifier.weight(1f),
                prominent = true,
                valueColor = deltaGhostColor(deltaGhostLabel),
            )
            MetricCard(
                title = "FC",
                value = heartRateLabel,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Card(
        modifier = modifier.height(132.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                modifier = Modifier.align(Alignment.CenterStart),
                style = if (prominent) {
                    MaterialTheme.typography.headlineLarge
                } else {
                    MaterialTheme.typography.headlineMedium
                },
                fontWeight = FontWeight.Bold,
                color = valueColor,
            )
        }
    }
}

@Composable
private fun SecondaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun DebugSection(
    uiState: CopilotUiState,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Debug brut",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(onClick = { expanded = !expanded }) {
                    Text(text = if (expanded) "Replier" else "Afficher")
                }
            }
            if (expanded) {
                HorizontalDivider()
                TelemetryRow("raceStarted", uiState.raceStarted)
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

@Composable
private fun deltaGhostColor(value: String): Color {
    val delta = value
        .replace(",", ".")
        .filter { it.isDigit() || it == '-' || it == '.' }
        .toDoubleOrNull()

    return when {
        delta == null -> MaterialTheme.colorScheme.onSurface
        delta > 1.0 -> Color(0xFF1B7F3A)
        delta < -1.0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Preview(showBackground = true)
@Composable
private fun CopilotScreenPreview() {
    AndroGustineCopiloteTheme {
        CopilotScreen(
            uiState = CopilotUiState(
                firestoreStatus = "Connecte - telemetry/latest actif",
                trackName = "Silesia Ring",
                sessionId = "demo-session",
                status = "RUNNING",
                raceStarted = "true",
                lapProgress = "2 / 11",
                sessionChrono = "04:21",
                lapChrono = "00:34",
                speedLabel = "5,4 km/h",
                heartRateLabel = "99 bpm",
                deltaGhostLabel = "-225 m",
                weatherLabel = "28,4 \u00B0C | vent 16,6 km/h | pluie 0 %",
                currentLap = "3",
                gpsSpeedKmh = "31.4",
                timestampIso = "2026-06-16T14:00:00Z",
            ),
        )
    }
}
