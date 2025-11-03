package veljko.najdanovic19273.fitmap.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import veljko.najdanovic19273.fitmap.data.model.MapObject
import veljko.najdanovic19273.fitmap.data.model.User
import veljko.najdanovic19273.fitmap.data.repository.UserRepository

class ProfileViewModel : ViewModel() {
    private val repository = UserRepository()

    data class ProfileUiState(
        val isLoading: Boolean = false,
        val user: User? = null,
        val userObjects: List<MapObject> = emptyList(),
        val userRank: Int? = null,
        val error: String? = null,
        val isUpdating: Boolean = false,
        val updateSuccess: Boolean = false
    )

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    companion object {
        private const val TAG = "ProfileViewModel"
    }

    init {
        loadCurrentUserProfile()
    }

    // Učitavanje profila trenutnog korisnika
    fun loadCurrentUserProfile() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val userResult = repository.getCurrentUser()

                if (userResult.isSuccess) {
                    val user = userResult.getOrNull()!!
                    _state.value = _state.value.copy(user = user)

                    // Učitaj objekte korisnika
                    loadUserObjects(user.id)

                    // Učitaj rang korisnika
                    loadUserRank(user.id)

                    _state.value = _state.value.copy(isLoading = false)
                } else {
                    val error = userResult.exceptionOrNull()?.message ?: "Greška pri učitavanju profila"
                    Log.e(TAG, "loadCurrentUserProfile failed: $error")
                    _state.value = _state.value.copy(error = error, isLoading = false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadCurrentUserProfile exception", e)
                _state.value = _state.value.copy(
                    error = e.message ?: "Nepoznata greška",
                    isLoading = false
                )
            }
        }
    }

    // Učitavanje profila određenog korisnika (za prikaz tuđeg profila)
    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val userResult = repository.getUserById(userId)

                if (userResult.isSuccess) {
                    val user = userResult.getOrNull()!!
                    _state.value = _state.value.copy(user = user)

                    loadUserObjects(userId)
                    loadUserRank(userId)

                    _state.value = _state.value.copy(isLoading = false)
                } else {
                    val error = userResult.exceptionOrNull()?.message ?: "Greška pri učitavanju profila"
                    Log.e(TAG, "loadUserProfile failed: $error")
                    _state.value = _state.value.copy(error = error, isLoading = false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadUserProfile exception", e)
                _state.value = _state.value.copy(
                    error = e.message ?: "Nepoznata greška",
                    isLoading = false
                )
            }
        }
    }

    // Učitavanje objekata korisnika
    private fun loadUserObjects(userId: String) {
        viewModelScope.launch {
            try {
                val objectsResult = repository.getUserObjects(userId)
                if (objectsResult.isSuccess) {
                    _state.value = _state.value.copy(userObjects = objectsResult.getOrNull() ?: emptyList())
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadUserObjects exception", e)
            }
        }
    }

    // Učitavanje ranga korisnika
    private fun loadUserRank(userId: String) {
        viewModelScope.launch {
            try {
                val rankResult = repository.getUserRank(userId)
                if (rankResult.isSuccess) {
                    _state.value = _state.value.copy(userRank = rankResult.getOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadUserRank exception", e)
            }
        }
    }

    // Ažuriranje profila
    fun updateProfile(
        firstName: String,
        lastName: String,
        username: String,
        phoneNumber: String,
        profileImageUrl: String? = null
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isUpdating = true, error = null, updateSuccess = false)
            try {
                val result = repository.updateProfile(
                    firstName = firstName,
                    lastName = lastName,
                    username = username,
                    phoneNumber = phoneNumber,
                    profileImageUrl = profileImageUrl
                )

                if (result.isSuccess) {
                    Log.d(TAG, "Profil uspešno ažuriran")
                    _state.value = _state.value.copy(isUpdating = false, updateSuccess = true)
                    // Reload profila da bi se prikazale nove vrednosti
                    loadCurrentUserProfile()
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Greška pri ažuriranju profila"
                    Log.e(TAG, "updateProfile failed: $error")
                    _state.value = _state.value.copy(error = error, isUpdating = false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "updateProfile exception", e)
                _state.value = _state.value.copy(
                    error = e.message ?: "Greška pri ažuriranju profila",
                    isUpdating = false
                )
            }
        }
    }

    // Reset update success stanja
    fun resetUpdateSuccess() {
        _state.value = _state.value.copy(updateSuccess = false)
    }

    // Clear error
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}

