package veljko.najdanovic19273.fitmap.ui.profile

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import veljko.najdanovic19273.fitmap.util.CloudinaryHelper
import veljko.najdanovic19273.fitmap.viewmodel.ProfileViewModel

private const val TAG = "EditProfileScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    profileViewModel: ProfileViewModel = viewModel()
) {
    val state by profileViewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State za input polja
    val firstNameState = remember { mutableStateOf("") }
    val lastNameState = remember { mutableStateOf("") }
    val usernameState = remember { mutableStateOf("") }
    val phoneState = remember { mutableStateOf("") }
    val imageUri = remember { mutableStateOf<Uri?>(null) }
    val isUploading = remember { mutableStateOf(false) }
    val localError = remember { mutableStateOf<String?>(null) }

    // Inicijalizuj polja sa trenutnim vrednostima
    LaunchedEffect(state.user) {
        state.user?.let { user ->
            firstNameState.value = user.firstName
            lastNameState.value = user.lastName
            usernameState.value = user.username
            phoneState.value = user.phoneNumber
        }
    }

    // Launcher za izbor slike
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri.value = uri
    }

    // Ako je update uspešan, vrati se nazad
    LaunchedEffect(state.updateSuccess) {
        if (state.updateSuccess) {
            Toast.makeText(context, "Profil uspešno ažuriran", Toast.LENGTH_SHORT).show()
            profileViewModel.resetUpdateSuccess()
            navController.popBackStack()
        }
    }

    // Prikaz grešaka
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            profileViewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Izmeni profil") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Nazad")
                    }
                }
            )
        }
    ) { padding ->
        if (state.user == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profilna slika
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        imageUri.value != null -> {
                            AsyncImage(
                                model = imageUri.value,
                                contentDescription = "Nova profilna slika",
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        state.user?.profileImageUrl?.isNotEmpty() == true -> {
                            AsyncImage(
                                model = state.user?.profileImageUrl,
                                contentDescription = "Profilna slika",
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Placeholder",
                                modifier = Modifier.size(60.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Dugme za promenu slike
                OutlinedButton(
                    onClick = { launcher.launch("image/*") },
                    enabled = !isUploading.value && !state.isUpdating
                ) {
                    Text(if (imageUri.value != null || state.user?.profileImageUrl?.isNotEmpty() == true) "Promeni sliku" else "Dodaj sliku")
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Input polja
                OutlinedTextField(
                    value = firstNameState.value,
                    onValueChange = { firstNameState.value = it },
                    label = { Text("Ime") },
                    singleLine = true,
                    enabled = !state.isUpdating && !isUploading.value,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = lastNameState.value,
                    onValueChange = { lastNameState.value = it },
                    label = { Text("Prezime") },
                    singleLine = true,
                    enabled = !state.isUpdating && !isUploading.value,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = usernameState.value,
                    onValueChange = { usernameState.value = it },
                    label = { Text("Korisničko ime") },
                    singleLine = true,
                    enabled = !state.isUpdating && !isUploading.value,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phoneState.value,
                    onValueChange = { phoneState.value = it },
                    label = { Text("Telefon") },
                    singleLine = true,
                    enabled = !state.isUpdating && !isUploading.value,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Email (read-only)
                OutlinedTextField(
                    value = state.user?.email ?: "",
                    onValueChange = {},
                    label = { Text("Email") },
                    singleLine = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Prikaz greške
                localError.value?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // Loading indikator
                if (state.isUpdating || isUploading.value) {
                    CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
                    Text(
                        text = if (isUploading.value) "Upload slike u toku..." else "Ažuriranje profila...",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Dugme za čuvanje
                Button(
                    onClick = {
                        scope.launch {
                            localError.value = null
                            var newProfileImageUrl: String? = null

                            try {
                                // Ako je izabrana nova slika, upload-uj je
                                if (imageUri.value != null) {
                                    isUploading.value = true
                                    Log.d(TAG, "Započinjem upload nove profilne slike...")

                                    val uploadResult = CloudinaryHelper.uploadImage(
                                        imageUri = imageUri.value!!,
                                        folder = "fitmap/profiles"
                                    )

                                    isUploading.value = false

                                    if (uploadResult.isSuccess) {
                                        newProfileImageUrl = uploadResult.getOrNull()
                                        Log.d(TAG, "Slika uspešno upload-ovana: $newProfileImageUrl")
                                    } else {
                                        val error = uploadResult.exceptionOrNull()?.message ?: "Greška pri upload-u slike"
                                        Log.e(TAG, "Upload neuspešan: $error")
                                        localError.value = error
                                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                        return@launch
                                    }
                                }

                                // Ažuriraj profil
                                profileViewModel.updateProfile(
                                    firstName = firstNameState.value.trim(),
                                    lastName = lastNameState.value.trim(),
                                    username = usernameState.value.trim(),
                                    phoneNumber = phoneState.value.trim(),
                                    profileImageUrl = newProfileImageUrl
                                )

                            } catch (e: Exception) {
                                isUploading.value = false
                                val errorMessage = e.message ?: "Nepoznata greška"
                                Log.e(TAG, "Greška: $errorMessage", e)
                                localError.value = errorMessage
                                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !state.isUpdating && !isUploading.value &&
                            firstNameState.value.isNotBlank() &&
                            lastNameState.value.isNotBlank() &&
                            usernameState.value.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sačuvaj izmene")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Dugme za otkazivanje
                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    enabled = !state.isUpdating && !isUploading.value,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Otkaži")
                }
            }
        }
    }
}
