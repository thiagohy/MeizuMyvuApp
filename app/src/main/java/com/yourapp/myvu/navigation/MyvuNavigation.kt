package com.yourapp.myvu.navigation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class RouteStep(
    val instruction: String,
    val distance: String,
    val duration: String,
    val startLocation: Location,
    val endLocation: Location
)

data class RouteResult(
    val summary: String,
    val totalDistance: String,
    val totalDuration: String,
    val steps: List<RouteStep>
)

interface LocationProvider {
    fun getCurrentLocation(callback: (Location?) -> Unit)
    fun startLocationUpdates(callback: (Location) -> Unit)
    fun stopLocationUpdates()
}

class GoogleFusedLocationProvider(
    private val context: Context
) : LocationProvider {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    private var locationCallback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    override fun getCurrentLocation(callback: (Location?) -> Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            callback(null)
            return
        }

        fusedClient.lastLocation.addOnSuccessListener { location ->
            callback(location)
        }.addOnFailureListener {
            callback(null)
        }
    }

    @SuppressLint("MissingPermission")
    override fun startLocationUpdates(callback: (Location) -> Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateDistanceMeters(5f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { callback(it) }
            }
        }

        fusedClient.requestLocationUpdates(request, locationCallback!!, Looper.getMainLooper())
    }

    override fun stopLocationUpdates() {
        locationCallback?.let { fusedClient.removeLocationUpdates(it) }
        locationCallback = null
    }
}

class GoogleDirectionsProvider(
    private val apiKey: String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun getRoute(
        origin: Location,
        destination: String,
        mode: String = "driving"
    ): RouteResult? = withContext(Dispatchers.IO) {
        try {
            val originStr = "${origin.latitude},${origin.longitude}"
            val url = "https://maps.googleapis.com/maps/api/directions/json" +
                    "?origin=$originStr" +
                    "&destination=${destination}" +
                    "&mode=$mode" +
                    "&language=pt-BR" +
                    "&key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext null

            val json = JSONObject(body)
            if (json.getString("status") != "OK") {
                return@withContext null
            }

            val routes = json.getJSONArray("routes")
            if (routes.length() == 0) return@withContext null

            val route = routes.getJSONObject(0)
            val leg = route.getJSONArray("legs").getJSONObject(0)

            val totalDistance = leg.getJSONObject("distance").getString("text")
            val totalDuration = leg.getJSONObject("duration").getString("text")
            val summary = route.getString("summary")

            val steps = mutableListOf<RouteStep>()
            val stepsArray = leg.getJSONArray("steps")

            for (i in 0 until stepsArray.length()) {
                val step = stepsArray.getJSONObject(i)
                val instruction = step.getString("html_instructions")
                    .replace("<b>", "")
                    .replace("</b>", "")
                    .replace("<div style=\"font-size:0.9em\">", " ")
                    .replace("</div>", "")
                    .replace("<wbr/>", "")

                val distance = step.getJSONObject("distance").getString("text")
                val duration = step.getJSONObject("duration").getString("text")

                val startLoc = Location("").apply {
                    val startJson = step.getJSONObject("start_location")
                    latitude = startJson.getDouble("lat")
                    longitude = startJson.getDouble("lng")
                }

                val endLoc = Location("").apply {
                    val endJson = step.getJSONObject("end_location")
                    latitude = endJson.getDouble("lat")
                    longitude = endJson.getDouble("lng")
                }

                steps.add(RouteStep(instruction, distance, duration, startLoc, endLoc))
            }

            RouteResult(summary, totalDistance, totalDuration, steps)

        } catch (e: Exception) {
            null
        }
    }

    suspend fun searchPlaces(
        query: String,
        location: Location,
        radius: Int = 5000
    ): List<PlaceResult> = withContext(Dispatchers.IO) {
        try {
            val locationStr = "${location.latitude},${location.longitude}"
            val url = "https://maps.googleapis.com/maps/api/place/textsearch/json" +
                    "?query=${query}" +
                    "&location=$locationStr" +
                    "&radius=$radius" +
                    "&language=pt-BR" +
                    "&key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext emptyList()

            val json = JSONObject(body)
            if (json.getString("status") != "OK") {
                return@withContext emptyList()
            }

            val results = json.getJSONArray("results")
            val places = mutableListOf<PlaceResult>()

            for (i in 0 until minOf(results.length(), 5)) {
                val place = results.getJSONObject(i)
                places.add(
                    PlaceResult(
                        name = place.getString("name"),
                        address = place.getString("formatted_address"),
                        location = Location("").apply {
                            val loc = place.getJSONObject("geometry").getJSONObject("location")
                            latitude = loc.getDouble("lat")
                            longitude = loc.getDouble("lng")
                        }
                    )
                )
            }

            places
        } catch (e: Exception) {
            emptyList()
        }
    }
}

data class PlaceResult(
    val name: String,
    val address: String,
    val location: Location
)

class MyvuNavigation(
    private val context: Context,
    private val directionsProvider: GoogleDirectionsProvider,
    private val locationProvider: LocationProvider,
    private val onStepUpdate: (String) -> Unit,
    private val onTurnUpdate: (String, Int) -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var currentRoute: RouteResult? = null
    private var currentStepIndex = 0
    private var isNavigating = false

    fun startNavigation(destination: String) {
        scope.launch {
            locationProvider.getCurrentLocation { location ->
                if (location == null) {
                    onStepUpdate("Erro: Localização não disponível")
                    return@getCurrentLocation
                }

                scope.launch {
                    val route = directionsProvider.getRoute(location, destination)
                    if (route == null) {
                        onStepUpdate("Erro: Rota não encontrada")
                        return@launch
                    }

                    currentRoute = route
                    currentStepIndex = 0
                    isNavigating = true

                    onStepUpdate("Navegando para: ${route.summary}")
                    onStepUpdate("Distância: ${route.totalDistance}")
                    onStepUpdate("Duração: ${route.totalDuration}")

                    updateStepDisplay()
                    startLocationTracking()
                }
            }
        }
    }

    private fun startLocationTracking() {
        locationProvider.startLocationUpdates { location ->
            if (!isNavigating) return@startLocationUpdates
            checkStepProgress(location)
        }
    }

    private fun checkStepProgress(currentLocation: Location) {
        val route = currentRoute ?: return
        if (currentStepIndex >= route.steps.size) {
            arriveAtDestination()
            return
        }

        val currentStep = route.steps[currentStepIndex]
        val distanceToStepEnd = currentLocation.distanceTo(currentStep.endLocation)

        if (distanceToStepEnd < 30f) {
            currentStepIndex++
            if (currentStepIndex >= route.steps.size) {
                arriveAtDestination()
            } else {
                updateStepDisplay()
            }
        }
    }

    private fun updateStepDisplay() {
        val route = currentRoute ?: return
        if (currentStepIndex >= route.steps.size) return

        val step = route.steps[currentStepIndex]
        val remaining = route.steps.size - currentStepIndex

        val turnDirection = getTurnDirection(step.instruction)
        onTurnUpdate(turnDirection, step.instruction.hashCode())

        onStepUpdate("${step.instruction}")
        onStepUpdate("${step.distance} - ${step.duration}")
        onStepUpdate("$remaining passos restantes")
    }

    private fun getTurnDirection(instruction: String): String {
        val lower = instruction.lowercase()
        return when {
            lower.contains("vire à direita") || lower.contains("turn right") -> "↗️ Direita"
            lower.contains("vire à esquerda") || lower.contains("turn left") -> "↖️ Esquerda"
            lower.contains("siga em frente") || lower.contains("continue") -> "⬆️ Reto"
            lower.contains("faça a volta") || lower.contains("u-turn") -> "🔄 Retorno"
            lower.contains("estination") || lower.contains("chegue") -> "📍 Chegada"
            else -> "⬆️ Siga"
        }
    }

    private fun arriveAtDestination() {
        isNavigating = false
        locationProvider.stopLocationUpdates()
        onStepUpdate("📍 Você chegou ao destino!")
        currentRoute = null
    }

    fun stopNavigation() {
        isNavigating = false
        locationProvider.stopLocationUpdates()
        currentRoute = null
        onStepUpdate("Navegação encerrada")
    }

    fun searchNearby(query: String) {
        scope.launch {
            locationProvider.getCurrentLocation { location ->
                if (location == null) {
                    onStepUpdate("Erro: Localização não disponível")
                    return@getCurrentLocation
                }

                scope.launch {
                    val places = directionsProvider.searchPlaces(query, location)
                    if (places.isEmpty()) {
                        onStepUpdate("Nenhum resultado para: $query")
                        return@launch
                    }

                    onStepUpdate("Resultados para: $query")
                    places.forEachIndexed { index, place ->
                        onStepUpdate("${index + 1}. ${place.name}")
                        onStepUpdate("   ${place.address}")
                    }
                }
            }
        }
    }

    fun destroy() {
        scope.cancel()
        locationProvider.stopLocationUpdates()
    }
}
