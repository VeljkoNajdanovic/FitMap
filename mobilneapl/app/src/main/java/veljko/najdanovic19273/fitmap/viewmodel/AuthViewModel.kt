package veljko.najdanovic19273.fitmap.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import veljko.najdanovic19273.fitmap.data.repository.AuthRepository

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    data class AuthUiState(
        val isLoading: Boolean = false,
        val user: FirebaseUser? = null,
        val error: String? = null
    )

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    companion object {
        private const val TAG = "AuthViewModel"
    }

    init {
        // Ako je korisnik već prijavljen, postavi ga u stanje
        repository.currentUser?.let { _state.value = _state.value.copy(user = it) }
    }

    fun register(
        email: String,
        password: String,
        username: String,
        firstName: String,
        lastName: String,
        phoneNumber: String,
        profileImageUrl: String = ""
    ) {
        Log.d(TAG, "register() called with email=$email username=$username")
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val result = repository.register(email, password, username, firstName, lastName, phoneNumber, profileImageUrl)
                if (result.isSuccess) {
                    Log.d(TAG, "register success: ${result.getOrNull()}")
                    _state.value = _state.value.copy(user = repository.currentUser, isLoading = false)
                } else {
                    val err = result.exceptionOrNull()?.message
                    Log.e(TAG, "register failed: $err")
                    _state.value = _state.value.copy(error = err, isLoading = false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "register exception", e)
                _state.value = _state.value.copy(error = e.message ?: "Greška pri registraciji", isLoading = false)
            }
        }
    }

    fun login(email: String, password: String) {
        Log.d(TAG, "login() called with email=$email")
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val result = repository.login(email, password)
                if (result.isSuccess) {
                    Log.d(TAG, "login success: ${result.getOrNull()?.email}")
                    _state.value = _state.value.copy(user = repository.currentUser, isLoading = false)
                } else {
                    val err = result.exceptionOrNull()?.message
                    Log.e(TAG, "login failed: $err")
                    _state.value = _state.value.copy(error = err, isLoading = false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "login exception", e)
                _state.value = _state.value.copy(error = e.message ?: "Greška pri prijavi", isLoading = false)
            }
        }
    }

    fun logout() {
        repository.logout()
        _state.value = _state.value.copy(user = null)
    }
}
