package com.yourapp.myvu.health

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.*

data class HealthData(
    val steps: Int = 0,
    val heartRate: Float = 0f,
    val calories: Float = 0f,
    val distance: Float = 0f,
    val bloodOxygen: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

interface HealthDataProvider {
    fun startListening()
    fun stopListening()
    fun setOnDataUpdateListener(listener: (HealthData) -> Unit)
    fun getCurrentData(): HealthData
}

class AndroidHealthDataProvider(
    context: Context
) : HealthDataProvider, SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var stepSensor: Sensor? = null
    private var heartRateSensor: Sensor? = null
    private var onDataUpdateListener: ((HealthData) -> Unit)? = null
    private var currentData = HealthData()
    private var initialSteps = 0

    init {
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
    }

    override fun startListening() {
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        heartRateSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun setOnDataUpdateListener(listener: (HealthData) -> Unit) {
        onDataUpdateListener = listener
    }

    override fun getCurrentData(): HealthData = currentData

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                val totalSteps = event.values[0].toInt()
                if (initialSteps == 0) initialSteps = totalSteps
                val stepsToday = totalSteps - initialSteps
                val calories = stepsToday * 0.04f
                val distance = stepsToday * 0.762f

                currentData = currentData.copy(
                    steps = stepsToday,
                    calories = calories,
                    distance = distance
                )
            }
            Sensor.TYPE_HEART_RATE -> {
                currentData = currentData.copy(
                    heartRate = event.values[0]
                )
            }
        }
        onDataUpdateListener?.invoke(currentData)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

class MyvuHealthMonitor(
    private val context: Context,
    private val onHealthUpdate: (String) -> Unit,
    private val onAlert: (String) -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val healthProvider = AndroidHealthDataProvider(context)
    private var monitoring = false
    private var lastStepCount = 0
    private var stepGoal = 10000

    private var heartRateHighThreshold = 100f
    private var heartRateLowThreshold = 50f
    private var lastAlertTime = 0L
    private val alertCooldown = 60000L

    fun startMonitoring() {
        monitoring = true
        lastStepCount = healthProvider.getCurrentData().steps

        healthProvider.setOnDataUpdateListener { data ->
            scope.launch {
                processHealthData(data)
            }
        }

        healthProvider.startListening()
        onHealthUpdate("Monitoramento de saúde iniciado")
    }

    fun stopMonitoring() {
        monitoring = false
        healthProvider.stopListening()
        onHealthUpdate("Monitoramento encerrado")
    }

    fun setStepGoal(goal: Int) {
        stepGoal = goal
        onHealthUpdate("Meta de passos: $goal")
    }

    fun setHeartRateThresholds(high: Float, low: Float) {
        heartRateHighThreshold = high
        heartRateLowThreshold = low
    }

    private fun processHealthData(data: HealthData) {
        if (!monitoring) return

        if (data.steps > lastStepCount + 100) {
            val percent = (data.steps * 100) / stepGoal
            onHealthUpdate("🚶 Passos: ${data.steps}/$stepGoal ($percent%)")
            lastStepCount = data.steps

            if (data.steps >= stepGoal) {
                onAlert("🎉 Parabéns! Você atingiu sua meta de passos!")
            }
        }

        if (data.heartRate > 0) {
            val now = System.currentTimeMillis()
            if (now - lastAlertTime > alertCooldown) {
                when {
                    data.heartRate > heartRateHighThreshold -> {
                        onAlert("⚠️ Batimento cardíaco alto: ${data.heartRate.toInt()} bpm")
                        lastAlertTime = now
                    }
                    data.heartRate < heartRateLowThreshold && data.heartRate > 0 -> {
                        onAlert("⚠️ Batimento cardíaco baixo: ${data.heartRate.toInt()} bpm")
                        lastAlertTime = now
                    }
                }
            }

            if (data.steps % 500 == 0 && data.steps > 0) {
                onHealthUpdate("❤️ Batimento: ${data.heartRate.toInt()} bpm")
            }
        }

        if (data.calories > 0 && data.steps % 1000 == 0 && data.steps > 0) {
            onHealthUpdate("🔥 Calorias: ${data.calories.toInt()} kcal")
            onHealthUpdate("📏 Distância: ${String.format("%.1f", data.distance / 1000)} km")
        }
    }

    fun getHealthSummary(): String {
        val data = healthProvider.getCurrentData()
        return """
            📊 Resumo de Saúde:
            🚶 Passos: ${data.steps}/$stepGoal
            ❤️ Batimento: ${data.heartRate.toInt()} bpm
            🔥 Calorias: ${data.calories.toInt()} kcal
            📏 Distância: ${String.format("%.2f", data.distance / 1000)} km
        """.trimIndent()
    }

    fun destroy() {
        scope.cancel()
        healthProvider.stopListening()
    }
}
