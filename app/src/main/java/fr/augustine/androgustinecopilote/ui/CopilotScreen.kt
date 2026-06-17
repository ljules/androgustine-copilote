package fr.augustine.androgustinecopilote.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.augustine.androgustinecopilote.data.CopilotInstructions
import fr.augustine.androgustinecopilote.data.StrategyData
import fr.augustine.androgustinecopilote.data.StrategySegment
import fr.augustine.androgustinecopilote.data.TrackData
import fr.augustine.androgustinecopilote.data.TrackPoint
import fr.augustine.androgustinecopilote.data.positionAtDistance
import fr.augustine.androgustinecopilote.ui.theme.AndroGustineCopiloteTheme
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun CopilotRoute(
    viewModel: CopilotViewModel = viewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    CopilotScreen(
        uiState = uiState.value,
        onPaceInstructionClick = viewModel::sendPaceInstruction,
        onRaceStatusInstructionClick = viewModel::sendRaceStatusInstruction,
        onPitStopRequestClick = viewModel::sendPitStopRequest,
    )
}

@Composable
fun CopilotScreen(
    uiState: CopilotUiState,
    modifier: Modifier = Modifier,
    onPaceInstructionClick: (String) -> Unit = {},
    onRaceStatusInstructionClick: (String) -> Unit = {},
    onPitStopRequestClick: (Boolean) -> Unit = {},
) {
    var mapMode by rememberSaveable { mutableStateOf(MapMode.Canvas) }

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
                MapModeSelector(
                    selectedMode = mapMode,
                    onModeSelected = { mapMode = it },
                )
                when (mapMode) {
                    MapMode.Canvas -> CircuitMapCard(
                        hasSession = uiState.hasSession,
                        track = uiState.track,
                        strategy = uiState.strategy,
                        currentLap = uiState.currentLapRaw,
                        snappedDistanceM = uiState.snappedDistanceMRaw,
                        ghostDistanceM = uiState.ghostDistanceMRaw,
                    )

                    MapMode.OpenStreetMap -> OpenStreetMapCard(
                        hasSession = uiState.hasSession,
                        track = uiState.track,
                        gpsLat = uiState.gpsLatRaw,
                        gpsLon = uiState.gpsLonRaw,
                        ghostDistanceM = uiState.ghostDistanceMRaw,
                    )
                }
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
                InstructionsCard(
                    uiState = uiState,
                    onPaceInstructionClick = onPaceInstructionClick,
                    onRaceStatusInstructionClick = onRaceStatusInstructionClick,
                    onPitStopRequestClick = onPitStopRequestClick,
                )
                DebugSection(uiState = uiState)
            }
        }
    }
}

@Composable
private fun InstructionsCard(
    uiState: CopilotUiState,
    onPaceInstructionClick: (String) -> Unit,
    onRaceStatusInstructionClick: (String) -> Unit,
    onPitStopRequestClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Consignes pilote",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            InstructionButtonGroup(
                title = "Cadence",
                options = listOf(
                    InstructionOption("Accelerer", CopilotInstructions.PACE_ACCELERATE),
                    InstructionOption("Maintenir", CopilotInstructions.PACE_MAINTAIN),
                    InstructionOption("Ralentir", CopilotInstructions.PACE_SLOW_DOWN),
                ),
                selectedValue = uiState.selectedPaceInstruction,
                enabled = !uiState.isSendingInstruction,
                onOptionClick = onPaceInstructionClick,
            )
            InstructionButtonGroup(
                title = "Etat course",
                options = listOf(
                    InstructionOption("Course", CopilotInstructions.STATUS_RACE),
                    InstructionOption("Ne pas doubler", CopilotInstructions.STATUS_NO_OVERTAKING),
                    InstructionOption("Stop", CopilotInstructions.STATUS_STOP),
                ),
                selectedValue = uiState.selectedRaceStatusInstruction,
                enabled = !uiState.isSendingInstruction,
                onOptionClick = onRaceStatusInstructionClick,
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Stand",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectableInstructionButton(
                        label = "Demande arret stand",
                        selected = uiState.pitStopRequest,
                        enabled = !uiState.isSendingInstruction,
                        onClick = { onPitStopRequestClick(true) },
                    )
                    SelectableInstructionButton(
                        label = "Annuler",
                        selected = !uiState.pitStopRequest,
                        enabled = !uiState.isSendingInstruction,
                        onClick = { onPitStopRequestClick(false) },
                    )
                }
            }
            HorizontalDivider()
            TelemetryRow("Derniere consigne", uiState.lastInstructionSent)
            TelemetryRow("Heure d'envoi", uiState.lastInstructionSentAt)
            if (uiState.isSendingInstruction) {
                Text(
                    text = "Envoi en cours...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            uiState.instructionErrorMessage?.let { errorMessage ->
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private data class InstructionOption(
    val label: String,
    val value: String,
)

private enum class MapMode {
    Canvas,
    OpenStreetMap,
}

@Composable
private fun MapModeSelector(
    selectedMode: MapMode,
    onModeSelected: (MapMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SelectableInstructionButton(
            label = "Vue circuit",
            selected = selectedMode == MapMode.Canvas,
            enabled = true,
            onClick = { onModeSelected(MapMode.Canvas) },
            modifier = Modifier.weight(1f),
        )
        SelectableInstructionButton(
            label = "Vue OSM",
            selected = selectedMode == MapMode.OpenStreetMap,
            enabled = true,
            onClick = { onModeSelected(MapMode.OpenStreetMap) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun InstructionButtonGroup(
    title: String,
    options: List<InstructionOption>,
    selectedValue: String,
    enabled: Boolean,
    onOptionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                SelectableInstructionButton(
                    label = option.label,
                    selected = option.value == selectedValue,
                    enabled = enabled,
                    onClick = { onOptionClick(option.value) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SelectableInstructionButton(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selected) {
        Button(
            modifier = modifier,
            enabled = enabled,
            onClick = onClick,
        ) {
            Text(text = label)
        }
    } else {
        OutlinedButton(
            modifier = modifier,
            enabled = enabled,
            onClick = onClick,
        ) {
            Text(text = label)
        }
    }
}

@Composable
private fun OpenStreetMapCard(
    hasSession: Boolean,
    track: TrackData?,
    gpsLat: Double?,
    gpsLon: Double?,
    ghostDistanceM: Double?,
    modifier: Modifier = Modifier,
) {
    val trackPoints = track?.points.orEmpty()
    val carPosition = if (gpsLat != null && gpsLon != null) {
        GeoPoint(gpsLat, gpsLon)
    } else {
        null
    }
    val ghostTrackPoint = positionAtDistance(
        points = trackPoints,
        distanceM = ghostDistanceM,
        totalDistanceM = track?.totalDistanceM,
    )
    val ghostPosition = ghostTrackPoint?.let { GeoPoint(it.lat, it.lon) }
    val messages = buildList {
        if (!hasSession || track == null || trackPoints.isEmpty()) add("Circuit non disponible")
        if (trackPoints.isNotEmpty() && carPosition == null) add("Position vehicule non disponible")
        if (trackPoints.isNotEmpty() && ghostPosition == null) add("Ghost non disponible")
    }
    val trackColor = MaterialTheme.colorScheme.primary.toArgb()
    val carColor = MaterialTheme.colorScheme.error.toArgb()
    val ghostColor = Color(0xFF1B7F3A).toArgb()
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "OpenStreetMap",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${trackPoints.size} pts",
                    style = MaterialTheme.typography.bodySmall,
                    color = mutedColor,
                )
            }
            if (trackPoints.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.7f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = messages.firstOrNull() ?: "Circuit non disponible",
                        style = MaterialTheme.typography.titleMedium,
                        color = mutedColor,
                    )
                }
            } else {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.7f),
                    factory = { context ->
                        Configuration.getInstance().userAgentValue = context.packageName
                        MapView(context).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(DEFAULT_OSM_ZOOM)
                        }
                    },
                    update = { mapView ->
                        mapView.overlays.clear()
                        val circuitGeoPoints = trackPoints
                            .sortedBy { it.distanceM }
                            .map { GeoPoint(it.lat, it.lon) }

                        val circuitLine = Polyline().apply {
                            setPoints(circuitGeoPoints)
                            outlinePaint.color = trackColor
                            outlinePaint.strokeWidth = OSM_TRACK_STROKE_WIDTH
                        }
                        mapView.overlays.add(circuitLine)

                        carPosition?.let { position ->
                            mapView.overlays.add(
                                Marker(mapView).apply {
                                    this.position = position
                                    title = "Voiture"
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                    icon?.setTint(carColor)
                                },
                            )
                        }

                        ghostPosition?.let { position ->
                            mapView.overlays.add(
                                Marker(mapView).apply {
                                    this.position = position
                                    title = "Ghost"
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                    icon?.setTint(ghostColor)
                                },
                            )
                        }

                        val boundsPoints = circuitGeoPoints + listOfNotNull(carPosition, ghostPosition)
                        if (boundsPoints.isNotEmpty()) {
                            mapView.post {
                                if (boundsPoints.size == 1) {
                                    mapView.controller.setCenter(boundsPoints.first())
                                    mapView.controller.setZoom(DEFAULT_OSM_ZOOM)
                                } else {
                                    val north = boundsPoints.maxOf { it.latitude }
                                    val south = boundsPoints.minOf { it.latitude }
                                    val east = boundsPoints.maxOf { it.longitude }
                                    val west = boundsPoints.minOf { it.longitude }
                                    mapView.zoomToBoundingBox(
                                        BoundingBox(north, east, south, west),
                                        false,
                                        OSM_BOUNDS_PADDING_PX,
                                    )
                                }
                            }
                        }
                        mapView.invalidate()
                    },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(
                    text = "Circuit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Voiture",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Ghost",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF1B7F3A),
                    fontWeight = FontWeight.SemiBold,
                )
            }
            messages.filterNot { it == "Circuit non disponible" && trackPoints.isEmpty() }.forEach { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = mutedColor,
                )
            }
        }
    }
}

@Composable
private fun CircuitMapCard(
    hasSession: Boolean,
    track: TrackData?,
    strategy: StrategyData?,
    currentLap: Long?,
    snappedDistanceM: Double?,
    ghostDistanceM: Double?,
    modifier: Modifier = Modifier,
) {
    val trackPoints = track?.points.orEmpty()
    val strategySegments = strategy.segmentsForLap(currentLap)
    val carPosition = positionAtDistance(
        points = trackPoints,
        distanceM = snappedDistanceM,
        totalDistanceM = track?.totalDistanceM,
    )
    val ghostPosition = positionAtDistance(
        points = trackPoints,
        distanceM = ghostDistanceM,
        totalDistanceM = track?.totalDistanceM,
    )
    val messages = buildList {
        if (!hasSession) add("Circuit non disponible")
        if (hasSession && track == null) add("Circuit non disponible")
        if (track != null && trackPoints.isEmpty()) add("Circuit non disponible")
        if (track != null && trackPoints.isNotEmpty() && snappedDistanceM == null) {
            add("Position vehicule non disponible")
        }
        if (track != null && trackPoints.isNotEmpty() && ghostDistanceM == null) {
            add("Ghost non disponible")
        }
    }
    val circuitColor = MaterialTheme.colorScheme.onSurfaceVariant
    val carColor = MaterialTheme.colorScheme.error
    val ghostColor = Color(0xFF1B7F3A)
    val mutedColor = MaterialTheme.colorScheme.onSurfaceVariant
    val segmentFallbackColors = listOf(
        Color.White,
        Color(0xFFFFD54F),
        Color(0xFF42A5F5),
        Color(0xFF66BB6A),
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Carte circuit",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${trackPoints.size} pts",
                    style = MaterialTheme.typography.bodySmall,
                    color = mutedColor,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.7f),
                contentAlignment = Alignment.Center,
            ) {
                if (trackPoints.isNotEmpty()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val paddingPx = 24.dp.toPx()
                        val minLon = trackPoints.minOf { it.lon }
                        val maxLon = trackPoints.maxOf { it.lon }
                        val minLat = trackPoints.minOf { it.lat }
                        val maxLat = trackPoints.maxOf { it.lat }
                        val refLon = (minLon + maxLon) / 2.0
                        val refLat = (minLat + maxLat) / 2.0
                        val meanLatRad = Math.toRadians(refLat)

                        fun TrackPoint.toLocalMeters(): Offset {
                            val x = ((lon - refLon) * LON_DEGREE_METERS * kotlin.math.cos(meanLatRad)).toFloat()
                            val y = ((lat - refLat) * LAT_DEGREE_METERS).toFloat()
                            return Offset(x, y)
                        }

                        val localTrackPoints = trackPoints.map { it to it.toLocalMeters() }
                        val localCarPosition = carPosition?.toLocalMeters()
                        val localGhostPosition = ghostPosition?.toLocalMeters()
                        val minX = localTrackPoints.minOf { it.second.x }
                        val maxX = localTrackPoints.maxOf { it.second.x }
                        val minY = localTrackPoints.minOf { it.second.y }
                        val maxY = localTrackPoints.maxOf { it.second.y }
                        val trackWidthM = (maxX - minX).takeIf { it > 0f } ?: 1f
                        val trackHeightM = (maxY - minY).takeIf { it > 0f } ?: 1f
                        val availableWidth = (size.width - (paddingPx * 2)).coerceAtLeast(1f)
                        val availableHeight = (size.height - (paddingPx * 2)).coerceAtLeast(1f)
                        val scale = minOf(
                            availableWidth / trackWidthM,
                            availableHeight / trackHeightM,
                        )
                        val drawingWidth = trackWidthM * scale
                        val drawingHeight = trackHeightM * scale
                        val offsetX = paddingPx + ((availableWidth - drawingWidth) / 2f)
                        val offsetY = paddingPx + ((availableHeight - drawingHeight) / 2f)

                        fun Offset.project(): Offset {
                            val x = offsetX + ((this.x - minX) * scale)
                            val y = offsetY + ((maxY - this.y) * scale)
                            return Offset(x, y)
                        }

                        val sortedPoints = localTrackPoints.sortedBy { it.first.distanceM }
                        val path = Path().apply {
                            val first = sortedPoints.first().second.project()
                            moveTo(first.x, first.y)
                            sortedPoints.drop(1).forEach { point ->
                                val projected = point.second.project()
                                lineTo(projected.x, projected.y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = circuitColor,
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                        )

                        strategySegments.forEachIndexed { index, segment ->
                            val ranges = segment.distanceRanges(track?.totalDistanceM)
                            ranges.forEach { range ->
                                val segmentPath = buildSegmentPath(
                                    startDistanceM = range.start,
                                    endDistanceM = range.endInclusive,
                                    points = trackPoints,
                                    totalDistanceM = track?.totalDistanceM,
                                    project = { toLocalMeters().project() },
                                )
                                val segmentColor = segment.toColor(segmentFallbackColors[index % segmentFallbackColors.size])
                                if (segmentColor == Color.White) {
                                    drawPath(
                                        path = segmentPath,
                                        color = Color(0xFF5F6368),
                                        style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round),
                                    )
                                }
                                drawPath(
                                    path = segmentPath,
                                    color = segmentColor,
                                    style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round),
                                )
                            }
                        }

                        localCarPosition?.project()?.let { projected ->
                            drawCircle(
                                color = carColor,
                                radius = 8.dp.toPx(),
                                center = projected,
                            )
                        }
                        localGhostPosition?.project()?.let { projected ->
                            drawCircle(
                                color = ghostColor,
                                radius = 9.dp.toPx(),
                                center = projected,
                                style = Stroke(width = 4.dp.toPx()),
                            )
                        }
                    }
                }
                if (trackPoints.isEmpty()) {
                    Text(
                        text = messages.firstOrNull() ?: "Circuit non disponible",
                        style = MaterialTheme.typography.titleMedium,
                        color = mutedColor,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(
                    text = "Segments",
                    style = MaterialTheme.typography.bodyMedium,
                    color = mutedColor,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Voiture",
                    style = MaterialTheme.typography.bodyMedium,
                    color = carColor,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Ghost",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ghostColor,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = "Segments strategie : ${strategySegments.size}",
                style = MaterialTheme.typography.bodySmall,
                color = mutedColor,
            )
            messages.filterNot { it == "Circuit non disponible" && trackPoints.isEmpty() }.forEach { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = mutedColor,
                )
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

private fun StrategyData?.segmentsForLap(currentLap: Long?): List<StrategySegment> {
    return when {
        this == null -> emptyList()
        currentLap == null -> startSegments
        currentLap <= 1L -> startSegments
        currentLap >= 2L -> raceSegments
        else -> emptyList()
    }
}

private fun StrategySegment.distanceRanges(totalDistanceM: Double?): List<ClosedFloatingPointRange<Double>> {
    val start = startDistanceM.coerceAtLeast(0.0)
    val end = endDistanceM.coerceAtLeast(0.0)
    return if (totalDistanceM != null && totalDistanceM > 0.0 && end < start) {
        listOf(start..totalDistanceM, 0.0..end)
    } else {
        listOf(start..end)
    }
}

private fun buildSegmentPath(
    startDistanceM: Double,
    endDistanceM: Double,
    points: List<TrackPoint>,
    totalDistanceM: Double?,
    project: TrackPoint.() -> Offset,
): Path {
    val path = Path()
    if (points.isEmpty()) return path

    val start = startDistanceM.coerceAtLeast(0.0)
    val end = endDistanceM.coerceAtLeast(start)
    val length = end - start
    val sampleCount = (length / SEGMENT_SAMPLE_STEP_M)
        .toInt()
        .coerceIn(MIN_SEGMENT_SAMPLES, MAX_SEGMENT_SAMPLES)
    val distances = (0..sampleCount).map { index ->
        start + ((length * index) / sampleCount)
    }

    distances.mapNotNull { distance ->
        positionAtDistance(
            points = points,
            distanceM = distance,
            totalDistanceM = totalDistanceM,
        )?.project()
    }.forEachIndexed { index, point ->
        if (index == 0) {
            path.moveTo(point.x, point.y)
        } else {
            path.lineTo(point.x, point.y)
        }
    }

    return path
}

private fun StrategySegment.toColor(fallbackColor: Color): Color {
    val key = (colorKey ?: label).orEmpty().uppercase()
    return when {
        key.contains("WHITE") || key.contains("BLANC") -> Color.White
        key.contains("YELLOW") || key.contains("JAUNE") -> Color(0xFFFFD54F)
        key.contains("BLUE") || key.contains("BLEU") -> Color(0xFF42A5F5)
        key.contains("GREEN") || key.contains("VERT") -> Color(0xFF66BB6A)
        else -> fallbackColor
    }
}

private const val SEGMENT_SAMPLE_STEP_M = 8.0
private const val MIN_SEGMENT_SAMPLES = 2
private const val MAX_SEGMENT_SAMPLES = 96
private const val DEFAULT_OSM_ZOOM = 17.0
private const val OSM_BOUNDS_PADDING_PX = 64
private const val OSM_TRACK_STROKE_WIDTH = 8f
private const val LON_DEGREE_METERS = 111_320.0
private const val LAT_DEGREE_METERS = 110_540.0

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
