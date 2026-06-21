package fr.augustine.androgustinecopilote.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.augustine.androgustinecopilote.data.CopilotInstructions
import fr.augustine.androgustinecopilote.data.StrategyData
import fr.augustine.androgustinecopilote.data.StrategySegment
import fr.augustine.androgustinecopilote.data.TrackData
import fr.augustine.androgustinecopilote.data.TrackPoint
import fr.augustine.androgustinecopilote.data.positionAtDistance
import fr.augustine.androgustinecopilote.ui.theme.AndroGustineCopiloteTheme
import fr.augustine.androgustinecopilote.ui.theme.DangerRed
import fr.augustine.androgustinecopilote.ui.theme.FlagGreen
import fr.augustine.androgustinecopilote.ui.theme.FlagYellow
import fr.augustine.androgustinecopilote.ui.theme.RacingCyan
import fr.augustine.androgustinecopilote.ui.theme.ShellOrange
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
import fr.augustine.androgustinecopilote.R
import fr.augustine.androgustinecopilote.ui.theme.OxaniumFontFamily
import kotlin.system.exitProcess

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
    CockpitScreenContent(
        uiState = uiState,
        modifier = modifier,
        onPaceInstructionClick = onPaceInstructionClick,
        onRaceStatusInstructionClick = onRaceStatusInstructionClick,
        onPitStopRequestClick = onPitStopRequestClick,
    )
    return
}

@Composable
private fun CockpitScreenContent(
    uiState: CopilotUiState,
    modifier: Modifier = Modifier,
    onPaceInstructionClick: (String) -> Unit,
    onRaceStatusInstructionClick: (String) -> Unit,
    onPitStopRequestClick: (Boolean) -> Unit,
) {
    var mapMode by rememberSaveable { mutableStateOf(MapMode.Canvas) }
    val context = LocalContext.current

    Scaffold(containerColor = Color.Black) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black),
        ) {
            Image(
                painter = painterResource(R.drawable.background_portrait_dark),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                CockpitHeader(uiState.firestoreStatus, uiState.firestoreConnectionIndicator, uiState.errorMessage)
                CockpitDivider()
                Spacer(Modifier.height(10.dp))
                CockpitTelemetryPanel(uiState)
                Spacer(Modifier.height(15.dp))
                CockpitDivider()
                CockpitWeatherAndHeartRowCorrected(uiState)
                CockpitDivider()
                Spacer(Modifier.height(20.dp))
                when (mapMode) {
                    MapMode.Canvas -> CockpitCircuitMapPanel(
                        hasSession = uiState.hasSession,
                        track = uiState.track,
                        strategy = uiState.strategy,
                        currentLap = uiState.currentLapRaw,
                        snappedDistanceM = uiState.snappedDistanceMRaw,
                        ghostDistanceM = uiState.ghostDistanceMRaw,
                    )

                    MapMode.OpenStreetMap -> CockpitOpenStreetMapPanel(
                        hasSession = uiState.hasSession,
                        track = uiState.track,
                        strategy = uiState.strategy,
                        currentLap = uiState.currentLapRaw,
                        gpsLat = uiState.gpsLatRaw,
                        gpsLon = uiState.gpsLonRaw,
                        snappedDistanceM = uiState.snappedDistanceMRaw,
                        ghostDistanceM = uiState.ghostDistanceMRaw,
                    )
                }
                MapModeSelector(selectedMode = mapMode, onModeSelected = { mapMode = it })

                Spacer(Modifier.height(30.dp))

                CockpitPaceInstructionPanelCorrected(
                    selectedValue = uiState.selectedPaceInstruction,
                    enabled = !uiState.isSendingInstruction,
                    onOptionClick = onPaceInstructionClick,
                )

                Spacer(Modifier.height(15.dp))
                CockpitDivider()
                Spacer(Modifier.height(15.dp))
                CockpitRaceStatusPanelCorrected(
                    selectedValue = uiState.selectedRaceStatusInstruction,
                    enabled = !uiState.isSendingInstruction,
                    onOptionClick = onRaceStatusInstructionClick,
                )
                Spacer(Modifier.height(15.dp))
                CockpitDivider()
                Spacer(Modifier.height(15.dp))
                CockpitPitStopPanel(
                    requested = uiState.pitStopRequest,
                    enabled = !uiState.isSendingInstruction,
                    onPitStopRequestClick = onPitStopRequestClick,
                )
                uiState.instructionErrorMessage?.let { error ->
                    Text(
                        text = error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        color = DangerRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            ExitApplicationButton(
                onClick = { context.closeApplication() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 12.dp),
            )
        }
    }
}

@Composable
private fun ExitApplicationButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = CircleShape
    var lastTapAtMs by rememberSaveable { mutableStateOf(0L) }
    Box(
        modifier = modifier
            .size(46.dp)
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.55f))
            .border(2.dp, ShellOrange, shape)
            .clickable {
                val now = System.currentTimeMillis()
                if (now - lastTapAtMs <= EXIT_DOUBLE_TAP_WINDOW_MS) {
                    onClick()
                } else {
                    lastTapAtMs = now
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "\u00D7",
            color = ShellOrange,
            fontSize = 34.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private const val EXIT_DOUBLE_TAP_WINDOW_MS = 2_000L

private fun Context.closeApplication() {
    findActivity()?.finishAndRemoveTask()
    exitProcess(0)
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

@Composable
private fun CockpitHeader(
    firestoreStatus: String,
    connectionIndicator: ConnectionIndicator,
    errorMessage: String?,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(connectionIndicator.toColor(), CircleShape),
            )
            Text(
                text = firestoreStatus,
                color = Color.White,
                fontSize = 12.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        errorMessage?.let {
            Text(
                text = it,
                color = DangerRed,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CockpitTelemetryPanel(uiState: CopilotUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CockpitIconValueRow(R.drawable.ico_loop, uiState.lapProgress.replace(" ", ""), 44.dp, 22)
            CockpitIconValueRow(R.drawable.ico_timer, uiState.sessionChrono, 46.dp, 22)
        }
        Column(
            modifier = Modifier.weight(0.85f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = uiState.speedLabel.replace("km/h", "").trim().substringBefore(",").substringBefore(".").ifBlank { "-" },
                color = Color.White,
                fontSize = 56.sp,
                lineHeight = 52.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
            )
            Text(
                text = "km/h",
                color = ShellOrange,
                fontSize = 18.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CockpitIconValueRow(R.drawable.ghost_distance, uiState.deltaGhostLabel, 44.dp, 20)
            CockpitIconValueRow(R.drawable.ico_energy, "0 J", 44.dp, 20)
        }
    }
}

@Composable
private fun CockpitIconValueRow(
    icon: Int,
    value: String,
    iconSize: Dp,
    valueSize: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            contentScale = ContentScale.Fit,
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = valueSize.sp,
            lineHeight = valueSize.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

@Composable
private fun CockpitWeatherAndHeartRowCorrected(uiState: CopilotUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .padding(horizontal = 34.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "\uD83C\uDF21\uFE0F${weatherTemperatureLabel(uiState.weatherTemperatureC)}",
            modifier = Modifier.weight(1.15f),
            color = Color.White,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
        Text(
            text = "\u224B${weatherWindLabel(uiState.weatherWindKmh)}",
            modifier = Modifier.weight(1.2f),
            color = Color.White,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
        Text(
            text = "\uD83D\uDCA7${formatRainPercent(uiState.weatherRainProbability)}",
            modifier = Modifier.weight(0.75f),
            color = Color.White,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
        Row(
            modifier = Modifier.weight(1.15f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.heart),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = uiState.heartRateLabel,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CockpitPaceInstructionPanelCorrected(
    selectedValue: String,
    enabled: Boolean,
    onOptionClick: (String) -> Unit,
) {
    CockpitInstructionPanelCorrected(icon = R.drawable.ico_speed_meter, height = 143.dp) {
        CockpitInstructionWithSpacerCorrected(
            label = "Acc\u00E9l\u00E9rer",
            selected = selectedValue == CopilotInstructions.PACE_ACCELERATE,
            color = DangerRed,
            enabled = enabled,
            onClick = { onOptionClick(CopilotInstructions.PACE_ACCELERATE) },
        )
        CockpitInstructionWithSpacerCorrected(
            label = "Maintenir",
            selected = selectedValue == CopilotInstructions.PACE_MAINTAIN,
            color = FlagGreen,
            enabled = enabled,
            onClick = { onOptionClick(CopilotInstructions.PACE_MAINTAIN) },
        )
        CockpitInstructionWithSpacerCorrected(
            label = "Ralentir",
            selected = selectedValue == CopilotInstructions.PACE_SLOW_DOWN,
            color = RacingCyan,
            enabled = enabled,
            onClick = { onOptionClick(CopilotInstructions.PACE_SLOW_DOWN) },
        )
    }
}

@Composable
private fun CockpitRaceStatusPanelCorrected(
    selectedValue: String,
    enabled: Boolean,
    onOptionClick: (String) -> Unit,
) {
    CockpitInstructionPanelCorrected(icon = null, height = 152.dp) {
        CockpitInstructionWithFlagCorrected(
            flag = R.drawable.flag_green,
            label = "Course",
            selected = selectedValue == CopilotInstructions.STATUS_RACE,
            color = FlagGreen,
            enabled = enabled,
            onClick = { onOptionClick(CopilotInstructions.STATUS_RACE) },
        )
        CockpitInstructionWithFlagCorrected(
            flag = R.drawable.flag_yellow,
            label = "Ne pas doubler",
            selected = selectedValue == CopilotInstructions.STATUS_NO_OVERTAKING,
            color = FlagYellow,
            enabled = enabled,
            onClick = { onOptionClick(CopilotInstructions.STATUS_NO_OVERTAKING) },
        )
        CockpitInstructionWithFlagCorrected(
            flag = R.drawable.flag_red,
            label = "Stop",
            selected = selectedValue == CopilotInstructions.STATUS_STOP,
            color = DangerRed,
            enabled = enabled,
            onClick = { onOptionClick(CopilotInstructions.STATUS_STOP) },
        )
    }
}

@Composable
private fun CockpitInstructionPanelCorrected(
    icon: Int?,
    height: Dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(82.dp),
            contentAlignment = Alignment.Center,
        ) {
            icon?.let {
                Image(
                    painter = painterResource(it),
                    contentDescription = null,
                    modifier = Modifier.size(70.dp),
                    contentScale = ContentScale.Fit,
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 32.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content,
        )
    }
}

@Composable
private fun CockpitInstructionWithFlagCorrected(
    flag: Int,
    label: String,
    selected: Boolean,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(flag),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(modifier = Modifier.width(30.dp))

        CockpitInstructionPillCorrected(
            label = label,
            selected = selected,
            color = color,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            onClick = onClick,
        )
    }
}

@Composable
private fun CockpitInstructionWithSpacerCorrected(
    label: String,
    selected: Boolean,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(38.dp).padding(end = 8.dp))
        CockpitInstructionPillCorrected(
            label = label,
            selected = selected,
            color = color,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            onClick = onClick,
        )
    }
}

@Composable
private fun CockpitInstructionPillCorrected(
    label: String,
    selected: Boolean,
    color: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(shape)
            .background(if (selected) color else Color.Transparent)
            .border(3.dp, color, shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else color,
            fontSize = 19.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CockpitWeatherAndHeartRow(uiState: CopilotUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .padding(horizontal = 34.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "T ${uiState.weatherTemperatureC} °C",
            modifier = Modifier.weight(1f),
            color = Color.White,
            fontSize = 14.sp,
            maxLines = 1,
        )
        Text(
            text = "≋ ${uiState.weatherWindKmh} km/h",
            modifier = Modifier.weight(1f),
            color = Color.White,
            fontSize = 14.sp,
            maxLines = 1,
        )
        Text(
            text = "Pluie ${formatRainPercent(uiState.weatherRainProbability)}",
            modifier = Modifier.weight(1f),
            color = Color.White,
            fontSize = 14.sp,
            maxLines = 1,
        )
        Row(
            modifier = Modifier.weight(1.25f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.heart),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = uiState.heartRateLabel,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CockpitPaceInstructionPanel(
    selectedValue: String,
    enabled: Boolean,
    onOptionClick: (String) -> Unit,
) {
    CockpitInstructionPanel(icon = R.drawable.ico_speed_meter, height = 143.dp) {
        CockpitInstructionPill("Accélérer", selectedValue == CopilotInstructions.PACE_ACCELERATE, FlagGreen, DangerRed, DangerRed, enabled) {
            onOptionClick(CopilotInstructions.PACE_ACCELERATE)
        }
        CockpitInstructionPill("Maintenir", selectedValue == CopilotInstructions.PACE_MAINTAIN, FlagGreen, FlagGreen, Color.White, enabled) {
            onOptionClick(CopilotInstructions.PACE_MAINTAIN)
        }
        CockpitInstructionPill("Ralentir", selectedValue == CopilotInstructions.PACE_SLOW_DOWN, FlagGreen, RacingCyan, RacingCyan, enabled) {
            onOptionClick(CopilotInstructions.PACE_SLOW_DOWN)
        }
    }
}

@Composable
private fun CockpitRaceStatusPanel(
    selectedValue: String,
    enabled: Boolean,
    onOptionClick: (String) -> Unit,
) {
    CockpitInstructionPanel(icon = null, height = 152.dp) {
        CockpitInstructionWithFlag(R.drawable.flag_green, "Course", selectedValue == CopilotInstructions.STATUS_RACE, FlagGreen, FlagGreen, Color.White, enabled) {
            onOptionClick(CopilotInstructions.STATUS_RACE)
        }
        CockpitInstructionWithFlag(R.drawable.flag_yellow, "Ne pas doubler", selectedValue == CopilotInstructions.STATUS_NO_OVERTAKING, FlagGreen, FlagYellow, FlagYellow, enabled) {
            onOptionClick(CopilotInstructions.STATUS_NO_OVERTAKING)
        }
        CockpitInstructionWithFlag(R.drawable.flag_red, "Stop", selectedValue == CopilotInstructions.STATUS_STOP, FlagGreen, DangerRed, DangerRed, enabled) {
            onOptionClick(CopilotInstructions.STATUS_STOP)
        }
    }
}

@Composable
private fun CockpitInstructionPanel(
    icon: Int?,
    height: Dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(82.dp),
            contentAlignment = Alignment.Center,
        ) {
            icon?.let {
                Image(
                    painter = painterResource(it),
                    contentDescription = null,
                    modifier = Modifier.size(70.dp),
                    contentScale = ContentScale.Fit,
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 72.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content,
        )
    }
}

@Composable
private fun CockpitInstructionWithFlag(
    flag: Int,
    label: String,
    selected: Boolean,
    selectedColor: Color,
    borderColor: Color,
    textColor: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(flag),
            contentDescription = null,
            modifier = Modifier.size(38.dp),
            contentScale = ContentScale.Fit,
        )
        CockpitInstructionPill(
            label = label,
            selected = selected,
            selectedColor = selectedColor,
            borderColor = borderColor,
            textColor = textColor,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            onClick = onClick,
        )
    }
}

@Composable
private fun CockpitInstructionPill(
    label: String,
    selected: Boolean,
    selectedColor: Color,
    borderColor: Color,
    textColor: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(shape)
            .background(if (selected) selectedColor else Color.Transparent)
            .border(3.dp, borderColor, shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else textColor,
            fontSize = 23.sp,
            lineHeight = 23.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CockpitPitStopPanel(
    requested: Boolean,
    enabled: Boolean,
    onPitStopRequestClick: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .padding(horizontal = 26.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ico_stands),
            contentDescription = null,
            modifier = Modifier.size(58.dp),
            contentScale = ContentScale.Fit,
        )
        Text(
            text = "stands",
            modifier = Modifier.padding(start = 10.dp),
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
        )
        CockpitPitSwitch(
            checked = requested,
            enabled = enabled,
            onClick = { onPitStopRequestClick(!requested) },
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

@Composable
private fun CockpitPitSwitch(
    checked: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .size(width = 43.dp, height = 21.dp)
            .clip(shape)
            .border(3.dp, ShellOrange, shape)
            .background(if (checked) ShellOrange else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .padding(2.dp)
                .size(13.dp)
                .background(ShellOrange, CircleShape),
        )
    }
}

@Composable
private fun CockpitCircuitMapPanel(
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
    val message = when {
        !hasSession || track == null || trackPoints.isEmpty() -> "Circuit non disponible"
        snappedDistanceM == null -> "Position véhicule non disponible"
        ghostDistanceM == null -> "Ghost non disponible"
        else -> null
    }
    val segmentFallbackColors = listOf(Color.White, FlagYellow, RacingCyan, FlagGreen)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(146.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (trackPoints.isNotEmpty()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val paddingPx = 18.dp.toPx()
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
                val availableWidth = (size.width - paddingPx * 2).coerceAtLeast(1f)
                val availableHeight = (size.height - paddingPx * 2).coerceAtLeast(1f)
                val scale = minOf(availableWidth / trackWidthM, availableHeight / trackHeightM)
                val drawingWidth = trackWidthM * scale
                val drawingHeight = trackHeightM * scale
                val offsetX = paddingPx + (availableWidth - drawingWidth) / 2f
                val offsetY = paddingPx + (availableHeight - drawingHeight) / 2f

                fun Offset.project(): Offset {
                    return Offset(
                        x = offsetX + ((x - minX) * scale),
                        y = offsetY + ((maxY - y) * scale),
                    )
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
                    color = Color.White,
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
                )

                strategySegments.forEachIndexed { index, segment ->
                    segment.distanceRanges(track?.totalDistanceM).forEach { range ->
                        val segmentPath = buildSegmentPath(
                            startDistanceM = range.start,
                            endDistanceM = range.endInclusive,
                            points = trackPoints,
                            totalDistanceM = track?.totalDistanceM,
                            project = { toLocalMeters().project() },
                        )
                        drawPath(
                            path = segmentPath,
                            color = segment.toColor(segmentFallbackColors[index % segmentFallbackColors.size]),
                            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }
                }

                localCarPosition?.project()?.let { projected ->
                    drawCircle(color = Color.White, radius = 11.dp.toPx(), center = projected)
                    drawCircle(color = GpsVehicleMarkerColor, radius = 9.dp.toPx(), center = projected)
                }
                localGhostPosition?.project()?.let { projected ->
                    drawCircle(
                        color = Color.White,
                        radius = 10.dp.toPx(),
                        center = projected,
                        style = Stroke(width = 2.dp.toPx()),
                    )
                    drawCircle(color = GhostMarkerColor, radius = 8.dp.toPx(), center = projected)
                }
            }
        }
        message?.let {
            Text(
                text = it,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CockpitOpenStreetMapPanel(
    hasSession: Boolean,
    track: TrackData?,
    strategy: StrategyData?,
    currentLap: Long?,
    gpsLat: Double?,
    gpsLon: Double?,
    snappedDistanceM: Double?,
    ghostDistanceM: Double?,
    modifier: Modifier = Modifier,
) {
    val trackPoints = track?.points.orEmpty()
    val strategySegments = strategy.segmentsForLap(currentLap)
    val snappedTrackPoint = positionAtDistance(
        points = trackPoints,
        distanceM = snappedDistanceM,
        totalDistanceM = track?.totalDistanceM,
    )
    val carPosition = snappedTrackPoint?.let { GeoPoint(it.lat, it.lon) }
        ?: if (gpsLat != null && gpsLon != null) GeoPoint(gpsLat, gpsLon) else null
    val ghostPosition = positionAtDistance(
        points = trackPoints,
        distanceM = ghostDistanceM,
        totalDistanceM = track?.totalDistanceM,
    )?.let { GeoPoint(it.lat, it.lon) }
    val message = when {
        !hasSession || track == null || trackPoints.isEmpty() -> "Circuit non disponible"
        carPosition == null -> "Position véhicule non disponible"
        ghostPosition == null -> "Ghost non disponible"
        else -> null
    }
    val segmentFallbackColors = listOf(Color.White, FlagYellow, RacingCyan, FlagGreen)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(146.dp)
            .clip(RoundedCornerShape(0.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (trackPoints.isNotEmpty()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
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

                    mapView.overlays.add(
                        Polyline().apply {
                            setPoints(circuitGeoPoints)
                            outlinePaint.color = Color.Black.toArgb()
                            outlinePaint.strokeWidth = OSM_TRACK_STROKE_WIDTH
                        },
                    )

                    strategySegments.forEachIndexed { index, segment ->
                        segment.distanceRanges(track?.totalDistanceM).forEach { range ->
                            val points = buildSegmentGeoPoints(
                                startDistanceM = range.start,
                                endDistanceM = range.endInclusive,
                                points = trackPoints,
                                totalDistanceM = track?.totalDistanceM,
                            )
                            if (points.size >= 2) {
                                mapView.overlays.add(
                                    Polyline().apply {
                                        setPoints(points)
                                        outlinePaint.color = segment.toColor(segmentFallbackColors[index % segmentFallbackColors.size]).toArgb()
                                        outlinePaint.strokeWidth = OSM_SEGMENT_STROKE_WIDTH
                                    },
                                )
                            }
                        }
                    }

                    carPosition?.let { position ->
                        mapView.overlays.add(
                            CircleMarkerOverlay(
                                position = position,
                                fillColor = GpsVehicleMarkerColor.toArgb(),
                                radiusPx = OSM_VEHICLE_MARKER_RADIUS_PX,
                            ),
                        )
                    }
                    ghostPosition?.let { position ->
                        mapView.overlays.add(
                            CircleMarkerOverlay(
                                position = position,
                                fillColor = GhostMarkerColor.toArgb(),
                                radiusPx = OSM_GHOST_MARKER_RADIUS_PX,
                            ),
                        )
                    }

                    if (circuitGeoPoints.isNotEmpty()) {
                        mapView.post {
                            if (circuitGeoPoints.size == 1) {
                                mapView.controller.setCenter(circuitGeoPoints.first())
                                mapView.controller.setZoom(DEFAULT_OSM_ZOOM)
                            } else {
                                mapView.zoomToBoundingBox(
                                    BoundingBox(
                                        circuitGeoPoints.maxOf { it.latitude },
                                        circuitGeoPoints.maxOf { it.longitude },
                                        circuitGeoPoints.minOf { it.latitude },
                                        circuitGeoPoints.minOf { it.longitude },
                                    ),
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
        message?.let {
            Text(
                text = it,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CockpitDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(ShellOrange),
    )
}

private fun formatRainPercent(value: String): String {
    val normalized = value.replace(",", ".").toDoubleOrNull()
    return when {
        normalized == null -> "$value %"
        normalized in 0.0..1.0 -> "${(normalized * 100).toInt()} %"
        else -> "${normalized.toInt()} %"
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




// ENUMERATIONS & CLASSES DE DONNEES :
// -----------------------------------
private fun weatherTemperatureLabel(value: String): String {
    return if (value.contains("°C") || value.contains("\u00B0C")) {
        value
    } else {
        "$value\u00B0C"
    }
}

private fun weatherWindLabel(value: String): String {
    return if (value.contains("km/h")) {
        value
    } else {
        "${value}km/h"
    }
}

private data class InstructionOption(
    val label: String,
    val value: String,
)

/**
 * Enumération des 2 modes d'affichage du circuit :
 */
private enum class MapMode {
    Canvas,
    OpenStreetMap,
}

// FONCTIONS UTILITAIRES & AFFICHAGE DES ELEMENTS :
// ------------------------------------------------

/**
 * Sélecteur de mode pour l'affichage de la carte de la piste
 */
@Composable
private fun MapModeSelector(
    selectedMode: MapMode,
    onModeSelected: (MapMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CockpitMapModeTab(
            label = "Vue circuit",
            selected = selectedMode == MapMode.Canvas,
            selectedColor = ShellOrange,
            onClick = { onModeSelected(MapMode.Canvas) },
            modifier = Modifier.weight(1f),
        )
        CockpitMapModeTab(
            label = "Vue OSM",
            selected = selectedMode == MapMode.OpenStreetMap,
            selectedColor = ShellOrange,
            onClick = { onModeSelected(MapMode.OpenStreetMap) },
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Interface de sélection des instructions.
 */
@Composable
private fun CockpitMapModeTab(
    label: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(if (selected) selectedColor else Color(0xFFC8C8C8))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
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

/**
 * Génère de l'affichage OpenStreetMap
 */
@Composable
private fun OpenStreetMapCard(
    hasSession: Boolean,
    track: TrackData?,
    strategy: StrategyData?,
    currentLap: Long?,
    gpsLat: Double?,
    gpsLon: Double?,
    snappedDistanceM: Double?,
    ghostDistanceM: Double?,
    modifier: Modifier = Modifier,
) {
    val trackPoints = track?.points.orEmpty()
    val strategySegments = strategy.segmentsForLap(currentLap)
    val snappedTrackPoint = positionAtDistance(
        points = trackPoints,
        distanceM = snappedDistanceM,
        totalDistanceM = track?.totalDistanceM,
    )
    val carPosition = snappedTrackPoint?.let { GeoPoint(it.lat, it.lon) }
        ?: if (gpsLat != null && gpsLon != null) GeoPoint(gpsLat, gpsLon) else null
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
    val trackColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val carColor = MaterialTheme.colorScheme.error.toArgb()
    val ghostColor = Color(0xFF1B7F3A).toArgb()
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

                        strategySegments.forEachIndexed { index, segment ->
                            val segmentColor = segment.toColor(segmentFallbackColors[index % segmentFallbackColors.size])
                            segment.distanceRanges(track?.totalDistanceM).forEach { range ->
                                val points = buildSegmentGeoPoints(
                                    startDistanceM = range.start,
                                    endDistanceM = range.endInclusive,
                                    points = trackPoints,
                                    totalDistanceM = track?.totalDistanceM,
                                )
                                if (points.size >= 2) {
                                    val strategyLine = Polyline().apply {
                                        setPoints(points)
                                        outlinePaint.color = segmentColor.toArgb()
                                        outlinePaint.strokeWidth = OSM_SEGMENT_STROKE_WIDTH
                                    }
                                    mapView.overlays.add(strategyLine)
                                }
                            }
                        }

                        carPosition?.let { position ->
                            mapView.overlays.add(
                                CircleMarkerOverlay(
                                    position = position,
                                    fillColor = GpsVehicleMarkerColor.toArgb(),
                                    radiusPx = OSM_VEHICLE_MARKER_RADIUS_PX,
                                ),
                            )
                        }

                        ghostPosition?.let { position ->
                            mapView.overlays.add(
                                CircleMarkerOverlay(
                                    position = position,
                                    fillColor = GhostMarkerColor.toArgb(),
                                    radiusPx = OSM_GHOST_MARKER_RADIUS_PX,
                                ),
                            )
                        }

                        if (circuitGeoPoints.isNotEmpty()) {
                            mapView.post {
                                if (circuitGeoPoints.size == 1) {
                                    mapView.controller.setCenter(circuitGeoPoints.first())
                                    mapView.controller.setZoom(DEFAULT_OSM_ZOOM)
                                } else {
                                    val north = circuitGeoPoints.maxOf { it.latitude }
                                    val south = circuitGeoPoints.minOf { it.latitude }
                                    val east = circuitGeoPoints.maxOf { it.longitude }
                                    val west = circuitGeoPoints.minOf { it.longitude }
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


/**
 * Génère le tracé du circuit
 */
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
    val carColor = GpsVehicleMarkerColor
    val ghostColor = GhostMarkerColor
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
                                color = Color.White,
                                radius = 10.dp.toPx(),
                                center = projected,
                            )
                            drawCircle(
                                color = GpsVehicleMarkerColor,
                                radius = 8.dp.toPx(),
                                center = projected,
                            )
                        }
                        localGhostPosition?.project()?.let { projected ->
                            drawCircle(
                                color = Color.White,
                                radius = 10.dp.toPx(),
                                center = projected,
                                style = Stroke(width = 2.dp.toPx()),
                            )
                            drawCircle(
                                color = GhostMarkerColor,
                                radius = 8.dp.toPx(),
                                center = projected,
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

/**
 * Génère l'affichage de l'entête
 */
@Composable
private fun DashboardHeader(
    firestoreStatus: String,
    connectionIndicator: ConnectionIndicator,
    errorMessage: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Titre principal (header) :
//        Text(
//            text = "AndroGustine Copilote",
//            style = MaterialTheme.typography.headlineMedium,
//            fontWeight = FontWeight.Bold,
//        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        color = connectionIndicator.toColor(),
                        shape = CircleShape,
                    ),
            )
            Text(
                text = firestoreStatus,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Light,
            )
        }
        errorMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

private fun ConnectionIndicator.toColor(): Color {
    return when (this) {
        ConnectionIndicator.Green -> FlagGreen
        ConnectionIndicator.Orange -> ShellOrange
        ConnectionIndicator.Red -> DangerRed
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

private fun buildSegmentGeoPoints(
    startDistanceM: Double,
    endDistanceM: Double,
    points: List<TrackPoint>,
    totalDistanceM: Double?,
): List<GeoPoint> {
    if (points.isEmpty()) return emptyList()

    val start = startDistanceM.coerceAtLeast(0.0)
    val end = endDistanceM.coerceAtLeast(start)
    val sortedPoints = points.sortedBy { it.distanceM }
    val segmentPoints = buildList {
        positionAtDistance(
            points = sortedPoints,
            distanceM = start,
            totalDistanceM = totalDistanceM,
        )?.let(::add)

        sortedPoints
            .filter { it.distanceM > start && it.distanceM < end }
            .forEach(::add)

        positionAtDistance(
            points = sortedPoints,
            distanceM = end,
            totalDistanceM = totalDistanceM,
        )?.let(::add)
    }

    return segmentPoints
        .distinctBy { "${it.lat}:${it.lon}" }
        .map { GeoPoint(it.lat, it.lon) }
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

private class CircleMarkerOverlay(
    private val position: GeoPoint,
    private val fillColor: Int,
    private val radiusPx: Float,
) : Overlay() {
    private val fillPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.FILL
        color = fillColor
    }
    private val strokePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.STROKE
        color = android.graphics.Color.WHITE
        strokeWidth = OSM_MARKER_STROKE_WIDTH_PX
    }
    private val projectedPoint = android.graphics.Point()

    override fun draw(
        canvas: android.graphics.Canvas,
        mapView: MapView,
        shadow: Boolean,
    ) {
        if (shadow) return

        mapView.projection.toPixels(position, projectedPoint)
        canvas.drawCircle(projectedPoint.x.toFloat(), projectedPoint.y.toFloat(), radiusPx, fillPaint)
        canvas.drawCircle(projectedPoint.x.toFloat(), projectedPoint.y.toFloat(), radiusPx, strokePaint)
    }
}

private const val SEGMENT_SAMPLE_STEP_M = 8.0
private const val MIN_SEGMENT_SAMPLES = 2
private const val MAX_SEGMENT_SAMPLES = 96
private const val DEFAULT_OSM_ZOOM = 17.0
private const val OSM_BOUNDS_PADDING_PX = 64
private const val OSM_TRACK_STROKE_WIDTH = 6f
private const val OSM_SEGMENT_STROKE_WIDTH = 12f
private const val OSM_VEHICLE_MARKER_RADIUS_PX = 10f
private const val OSM_GHOST_MARKER_RADIUS_PX = 10f
private const val OSM_MARKER_STROKE_WIDTH_PX = 3f
private const val LON_DEGREE_METERS = 111_320.0
private const val LAT_DEGREE_METERS = 110_540.0
private val GpsVehicleMarkerColor = Color(0xFFE50914)
private val GhostMarkerColor = Color(0x66B45AE8)

@Preview(showBackground = true)
@Composable
private fun CopilotScreenPreview() {
    AndroGustineCopiloteTheme {
        CopilotScreen(
            uiState = CopilotUiState(
                firestoreStatus = "Connexion en cours...",
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
