package fr.augustine.androgustinecopilote.data

data class StrategySegment(
    val startDistanceM: Double,
    val endDistanceM: Double,
    val colorKey: String? = null,
    val label: String? = null,
)

data class StrategyData(
    val startSegments: List<StrategySegment> = emptyList(),
    val raceSegments: List<StrategySegment> = emptyList(),
)
