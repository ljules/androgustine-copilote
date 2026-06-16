package fr.augustine.androgustinecopilote.data

fun positionAtDistance(
    points: List<TrackPoint>,
    distanceM: Double?,
    totalDistanceM: Double? = null,
): TrackPoint? {
    if (distanceM == null || points.isEmpty()) return null

    val sortedPoints = points.sortedBy { it.distanceM }
    val normalizedDistance = if (totalDistanceM != null && totalDistanceM > 0.0) {
        distanceM.mod(totalDistanceM)
    } else {
        distanceM
    }

    if (normalizedDistance <= sortedPoints.first().distanceM) return sortedPoints.first()
    if (normalizedDistance >= sortedPoints.last().distanceM) return sortedPoints.last()

    val nextIndex = sortedPoints.indexOfFirst { it.distanceM >= normalizedDistance }
    if (nextIndex <= 0) return sortedPoints.first()

    val previous = sortedPoints[nextIndex - 1]
    val next = sortedPoints[nextIndex]
    val segmentDistance = next.distanceM - previous.distanceM
    if (segmentDistance <= 0.0) return previous

    val ratio = (normalizedDistance - previous.distanceM) / segmentDistance
    return TrackPoint(
        distanceM = normalizedDistance,
        lat = previous.lat + ((next.lat - previous.lat) * ratio),
        lon = previous.lon + ((next.lon - previous.lon) * ratio),
    )
}
