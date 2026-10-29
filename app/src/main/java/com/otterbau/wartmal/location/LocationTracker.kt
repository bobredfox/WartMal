package com.otterbau.wartmal.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.otterbau.wartmal.data.GeoPoint
import java.util.function.Consumer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DevicePosition(
    val point: GeoPoint,
    val accuracyMeters: Float
)

class LocationTracker(private val context: Context) : SensorEventListener {

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val _position = MutableStateFlow<DevicePosition?>(null)
    val position: StateFlow<DevicePosition?> = _position.asStateFlow()

    private val _deviceAzimuth = MutableStateFlow(0f)
    val deviceAzimuth: StateFlow<Float> = _deviceAzimuth.asStateFlow()

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private val accelerometerValues = FloatArray(3)
    private val magnetometerValues = FloatArray(3)
    private var hasAccelerometer = false
    private var hasMagnetometer = false

    private var listening = false

    val hasLocationPermission: Boolean
        get() = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            _position.value = DevicePosition(
                point = GeoPoint(location.latitude, location.longitude),
                accuracyMeters = location.accuracy
            )
        }

        override fun onProviderDisabled(provider: String) {}

        override fun onProviderEnabled(provider: String) {}

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (listening) return
        listening = true
        if (hasLocationPermission) {
            val providers = locationManager.getProviders(true)
            val provider = when {
                providers.contains(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                providers.contains(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                else -> null
            }
            if (provider != null) {
                runCatching {
                    locationManager.getCurrentLocation(
                        provider,
                        android.os.CancellationSignal(),
                        ContextCompat.getMainExecutor(context),
                        Consumer<Location> { location ->
                            locationListener.onLocationChanged(location)
                        }
                    )
                }
                runCatching {
                    locationManager.requestLocationUpdates(
                        provider,
                        MIN_UPDATE_INTERVAL_MILLIS,
                        MIN_UPDATE_DISTANCE_METERS,
                        locationListener,
                        Looper.getMainLooper()
                    )
                }
            }
        }
        registerCompassSensors()
    }

    fun stop() {
        if (!listening) return
        listening = false
        runCatching { locationManager.removeUpdates(locationListener) }
        sensorManager.unregisterListener(this)
    }

    private fun registerCompassSensors() {
        val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rotationVector != null) {
            sensorManager.registerListener(
                this,
                rotationVector,
                SensorManager.SENSOR_DELAY_UI
            )
            return
        }
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (accelerometer != null) {
            hasAccelerometer = sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        if (magnetometer != null) {
            hasMagnetometer = sensorManager.registerListener(
                this,
                magnetometer,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                updateAzimuth(orientationAngles[0])
            }

            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelerometerValues, 0, 3)
            }

            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetometerValues, 0, 3)
            }
        }
        if (hasAccelerometer && hasMagnetometer &&
            event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD
        ) {
            val r = FloatArray(9)
            val i = FloatArray(9)
            if (SensorManager.getRotationMatrix(
                    r,
                    i,
                    accelerometerValues,
                    magnetometerValues
                )
            ) {
                SensorManager.getOrientation(r, orientationAngles)
                updateAzimuth(orientationAngles[0])
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun updateAzimuth(radians: Float) {
        var azimuth = Math.toDegrees(radians.toDouble()).toFloat()
        azimuth = (azimuth + 360f) % 360f
        val declination = try {
            val now = System.currentTimeMillis()
            val last = _position.value
            if (last != null) {
                GeomagneticField(
                    last.point.latitude.toFloat(),
                    last.point.longitude.toFloat(),
                    0f,
                    now
                ).declination
            } else {
                0f
            }
        } catch (_: Exception) {
            0f
        }
        _deviceAzimuth.value = (azimuth + declination + 360f) % 360f
    }

    companion object {
        private const val MIN_UPDATE_INTERVAL_MILLIS = 2_000L
        private const val MIN_UPDATE_DISTANCE_METERS = 2f
    }
}
