package veljko.najdanovic19273.fitmap.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import veljko.najdanovic19273.fitmap.service.LocationTrackingService

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    data class LocationServiceState(
        val isServiceRunning: Boolean = false,
        val error: String? = null
    )

    private val _state = MutableStateFlow(LocationServiceState())
    val state: StateFlow<LocationServiceState> = _state.asStateFlow()

    // Pokreni location tracking service
    fun startLocationTracking() {
        viewModelScope.launch {
            try {
                android.util.Log.d("LocationViewModel", "═══════════════════════���═══════════════")
                android.util.Log.d("LocationViewModel", "🚀 Pokrećem LocationTrackingService...")

                val intent = Intent(getApplication<Application>().applicationContext, LocationTrackingService::class.java)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    android.util.Log.d("LocationViewModel", "   Koristim startForegroundService() za Android 8+")
                    getApplication<Application>().applicationContext.startForegroundService(intent)
                } else {
                    android.util.Log.d("LocationViewModel", "   Koristim startService()")
                    getApplication<Application>().applicationContext.startService(intent)
                }

                _state.value = _state.value.copy(
                    isServiceRunning = true,
                    error = null
                )

                android.util.Log.d("LocationViewModel", "✅ Servis pokrenut uspešno!")
                android.util.Log.d("LocationViewModel", "═══════════════════════════════════════")
            } catch (e: Exception) {
                android.util.Log.e("LocationViewModel", "❌ GREŠKA pri pokretanju servisa: ${e.message}", e)
                _state.value = _state.value.copy(
                    isServiceRunning = false,
                    error = "Greška pri pokretanju servisa: ${e.message}"
                )
            }
        }
    }

    // Zaustavi location tracking service
    fun stopLocationTracking() {
        viewModelScope.launch {
            try {
                android.util.Log.d("LocationViewModel", "🛑 Zaustavljam LocationTrackingService...")

                val intent = Intent(getApplication<Application>().applicationContext, LocationTrackingService::class.java)
                getApplication<Application>().applicationContext.stopService(intent)

                _state.value = _state.value.copy(
                    isServiceRunning = false,
                    error = null
                )

                android.util.Log.d("LocationViewModel", "✅ Servis zaustavljen!")
            } catch (e: Exception) {
                android.util.Log.e("LocationViewModel", "❌ GREŠKA pri zaustavljanju servisa: ${e.message}", e)
                _state.value = _state.value.copy(
                    error = "Greška pri zaustavljanju servisa: ${e.message}"
                )
            }
        }
    }
}
