package com.otterbau.wartmal

import com.otterbau.wartmal.data.GeoPoint
import com.otterbau.wartmal.data.bearingToDirection
import com.otterbau.wartmal.data.formatDistance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class GeoPointTest {

    private val berlin = GeoPoint(latitude = 52.520008, longitude = 13.404954)
    private val munich = GeoPoint(latitude = 48.137154, longitude = 11.576124)

    @Test
    fun distance_berlinToMunich_isApproximately504Km() {
        val distance = berlin.distanceToMeters(munich)
        assertTrue("distance was $distance", distance in 500_000.0..510_000.0)
    }

    @Test
    fun distance_toSamePoint_isZero() {
        assertEquals(0.0, berlin.distanceToMeters(berlin), 0.0001)
    }

    @Test
    fun distance_isSymmetric() {
        assertEquals(
            berlin.distanceToMeters(munich),
            munich.distanceToMeters(berlin),
            0.0001
        )
    }

    @Test
    fun distance_oneDegreeLatitude_isAbout111Km() {
        val oneDegreeNorth = GeoPoint(latitude = 53.520008, longitude = 13.404954)
        val distance = berlin.distanceToMeters(oneDegreeNorth)
        assertTrue("distance was $distance", distance in 110_000.0..112_000.0)
    }

    @Test
    fun initialBearing_berlinToMunich_isSouth() {
        val bearing = berlin.initialBearingTo(munich)
        assertTrue("bearing was $bearing", bearing in 180f..225f)
        assertEquals("S", bearingToDirection(bearing))
    }

    @Test
    fun initialBearing_dueEast_is90Degrees() {
        val east = GeoPoint(latitude = 52.520008, longitude = 14.404954)
        assertEquals(90.0, berlin.initialBearingTo(east).toDouble(), 1.0)
        assertEquals("E", bearingToDirection(berlin.initialBearingTo(east)))
    }

    @Test
    fun initialBearing_dueNorth_is0Degrees() {
        val north = GeoPoint(latitude = 53.520008, longitude = 13.404954)
        assertEquals(0.0, berlin.initialBearingTo(north).toDouble(), 1.0)
        assertEquals("N", bearingToDirection(berlin.initialBearingTo(north)))
    }

    @Test
    fun bearingToDirection_wrapsAround360() {
        assertEquals("N", bearingToDirection(359f))
        assertEquals("N", bearingToDirection(0f))
        assertEquals("NW", bearingToDirection(315f))
        assertEquals("S", bearingToDirection(180f))
    }

    @Test
    fun formatDistance_metersBelowOneKm() {
        assertEquals("0 m", formatDistance(0.4))
        assertEquals("250 m", formatDistance(250.0))
        assertEquals("999 m", formatDistance(999.4))
    }

    @Test
    fun formatDistance_kilometersAboveOneKm() {
        assertEquals("1.0 km", formatDistance(1000.0))
        assertEquals("12.3 km", formatDistance(12_340.0))
    }

    @Test
    fun geoPoint_rejectsOutOfRangeLatitude() {
        try {
            GeoPoint(latitude = 90.1, longitude = 0.0)
            fail("Expected IllegalArgumentException for latitude > 90")
        } catch (expected: IllegalArgumentException) {
        }
        try {
            GeoPoint(latitude = -90.1, longitude = 0.0)
            fail("Expected IllegalArgumentException for latitude < -90")
        } catch (expected: IllegalArgumentException) {
        }
    }

    @Test
    fun geoPoint_rejectsOutOfRangeLongitude() {
        try {
            GeoPoint(latitude = 0.0, longitude = 180.1)
            fail("Expected IllegalArgumentException for longitude > 180")
        } catch (expected: IllegalArgumentException) {
        }
        try {
            GeoPoint(latitude = 0.0, longitude = -180.1)
            fail("Expected IllegalArgumentException for longitude < -180")
        } catch (expected: IllegalArgumentException) {
        }
    }

    @Test
    fun geoPoint_acceptsBoundaryValues() {
        GeoPoint(latitude = -90.0, longitude = -180.0)
        GeoPoint(latitude = 90.0, longitude = 180.0)
    }
}
