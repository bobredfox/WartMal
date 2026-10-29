package com.otterbau.wartmal.data

import java.util.Locale
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_METERS = 6_371_000.0

private fun Double.toRadians(): Double = this * PI / 180.0

private fun Double.toDegrees(): Double = this * 180.0 / PI

data class GeoPoint(val latitude: Double, val longitude: Double) {

    init {
        require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180" }
    }

    fun distanceToMeters(other: GeoPoint): Double {
        val dLat = (other.latitude - latitude).toRadians()
        val dLon = (other.longitude - longitude).toRadians()
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(latitude.toRadians()) * cos(other.latitude.toRadians()) *
            sin(dLon / 2) * sin(dLon / 2)
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(a))
    }

    fun initialBearingTo(other: GeoPoint): Float {
        val lat1 = latitude.toRadians()
        val lat2 = other.latitude.toRadians()
        val dLon = (other.longitude - longitude).toRadians()
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return ((atan2(y, x).toDegrees() + 360.0) % 360.0).toFloat()
    }
}

fun formatDistance(meters: Double): String =
    if (meters < 1000) {
        "${meters.roundToInt()} m"
    } else {
        String.format(Locale.US, "%.1f km", meters / 1000.0)
    }

fun bearingToDirection(bearing: Float): String {
    val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val normalized = ((bearing % 360f) + 360f) % 360f
    val index = ((normalized + 22.5f) / 45f).toInt() % 8
    return directions[index]
}
