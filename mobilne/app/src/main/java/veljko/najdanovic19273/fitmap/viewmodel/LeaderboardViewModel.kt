package veljko.najdanovic19273.fitmap.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import veljko.najdanovic19273.fitmap.data.model.User
import veljko.najdanovic19273.fitmap.util.Constants

class LeaderboardViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    data class LeaderboardUiState(
        val isLoading: Boolean = false,
        val users: List<User> = emptyList(),
        val error: String? = null
    )

    private val _state = MutableStateFlow(LeaderboardUiState())
    val state: StateFlow<LeaderboardUiState> = _state.asStateFlow()

    companion object {
        private const val TAG = "LeaderboardViewModel"
    }

    // Učitavanje leaderboard-a (korisnici sortirani po poenima)
    fun loadLeaderboard() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                Log.d(TAG, "Učitavam leaderboard...")

                // Učitaj SVE korisnike bez orderBy (izbegavamo potrebu za indeksom)
                val snapshot = firestore.collection(Constants.USERS_COLLECTION)
                    .get()
                    .await()

                // Sortiraj na klijentskoj strani
                val users = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(User::class.java)?.copy(id = doc.id)
                }.sortedByDescending { it.points } // Sortiraj po poenima
                  .take(100) // Top 100 korisnika

                Log.d(TAG, "Učitano ${users.size} korisnika na leaderboard-u")
                _state.value = _state.value.copy(
                    users = users,
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "Greška pri učitavanju leaderboard-a", e)
                _state.value = _state.value.copy(
                    error = e.message ?: "Greška pri učitavanju leaderboard-a",
                    isLoading = false
                )
            }
        }
    }
}
