package com.bolpaisa.app.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(private val onShakeDetected: () -> Unit) : SensorEventListener {

    companion object {
        private const val SHAKE_THRESHOLD_M_S2 = 13.0f // ~12-14 m/s²
        private const val DEBOUNCE_TIME_MS = 1500L
    }

    private var lastShakeTimestamp = 0L

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val acceleration = sqrt(x * x + y * y + z * z)

        if (acceleration >= SHAKE_THRESHOLD_M_S2) {
            val now = System.currentTimeMillis()
            if (now - lastShakeTimestamp >= DEBOUNCE_TIME_MS) {
                lastShakeTimestamp = now
                onShakeDetected()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
