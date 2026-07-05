package veljko.najdanovic19273.fitmap.ui.auth

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import veljko.najdanovic19273.fitmap.ui.theme.*
import veljko.najdanovic19273.fitmap.util.CloudinaryHelper
import veljko.najdanovic19273.fitmap.viewmodel.AuthViewModel

private const val TAG = "RegisterScreen"

@Composable
fun RegisterScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    val emailState = remember { mutableStateOf("") }
    val passwordState = remember { mutableStateOf("") }
    val usernameState = remember { mutableStateOf("") }
    val firstNameState = remember { mutableStateOf("") }
    val lastNameState = remember { mutableStateOf("") }
    val phoneState = remember { mutableStateOf("") }
    val passwordVisible = remember { mutableStateOf(false) }

    val state by authViewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val imageUri = remember { mutableStateOf<Uri?>(null) }
    val localError = remember { mutableStateOf<String?>(null) }
    val isUploading = remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri.value = uri
    }

    LaunchedEffect(state.user) {
        state.user?.let { user ->
            Log.d(TAG, "Registracija uspešna, korisnik: ${user.email}")
            Toast.makeText(context, "Dobrodošli!", Toast.LENGTH_SHORT).show()
            navController.navigate("home") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { err ->
            Log.e(TAG, "Auth error: $err")
            Toast.makeText(context, "Greška: $err", Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        GradientDarkStart,
                        GradientDarkMid,
                        DarkBackground
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Kreiraj Nalog",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )

            Text(
                text = "Započni svoj fitness journey",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Profilna slika - elegantan dizajn
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(DarkCard)
                    .border(2.dp, GoldPrimary, CircleShape)
                    .clickable { launcher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri.value != null) {
                    AsyncImage(
                        model = imageUri.value,
                        contentDescription = "Profilna slika",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = "Dodaj sliku",
                        modifier = Modifier.size(40.dp),
                        tint = GoldPrimary
                    )
                }
            }

            TextButton(
                onClick = { launcher.launch("image/*") },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = if (imageUri.value != null) "Promeni sliku" else "Dodaj sliku",
                    color = GoldPrimary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DarkSurface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Email
                    OutlinedTextField(
                        value = emailState.value,
                        onValueChange = { emailState.value = it },
                        label = { Text("Email") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, null, tint = GoldPrimary)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = TextDisabled,
                            focusedLabelColor = GoldPrimary,
                            unfocusedLabelColor = TextSecondary,
                            cursorColor = GoldPrimary,
                            focusedContainerColor = DarkCard,
                            unfocusedContainerColor = DarkCard
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Lozinka
                    OutlinedTextField(
                        value = passwordState.value,
                        onValueChange = { passwordState.value = it },
                        label = { Text("Lozinka") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, null, tint = GoldPrimary)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible.value = !passwordVisible.value }) {
                                Icon(
                                    if (passwordVisible.value) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    null,
                                    tint = TextSecondary
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = TextDisabled,
                            focusedLabelColor = GoldPrimary,
                            unfocusedLabelColor = TextSecondary,
                            cursorColor = GoldPrimary,
                            focusedContainerColor = DarkCard,
                            unfocusedContainerColor = DarkCard
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Korisničko ime
                    OutlinedTextField(
                        value = usernameState.value,
                        onValueChange = { usernameState.value = it },
                        label = { Text("Korisničko ime") },
                        leadingIcon = {
                            Icon(Icons.Default.AccountCircle, null, tint = GoldPrimary)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = TextDisabled,
                            focusedLabelColor = GoldPrimary,
                            unfocusedLabelColor = TextSecondary,
                            cursorColor = GoldPrimary,
                            focusedContainerColor = DarkCard,
                            unfocusedContainerColor = DarkCard
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Ime
                    OutlinedTextField(
                        value = firstNameState.value,
                        onValueChange = { firstNameState.value = it },
                        label = { Text("Ime") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, null, tint = GoldPrimary)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = TextDisabled,
                            focusedLabelColor = GoldPrimary,
                            unfocusedLabelColor = TextSecondary,
                            cursorColor = GoldPrimary,
                            focusedContainerColor = DarkCard,
                            unfocusedContainerColor = DarkCard
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Prezime
                    OutlinedTextField(
                        value = lastNameState.value,
                        onValueChange = { lastNameState.value = it },
                        label = { Text("Prezime") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, null, tint = GoldPrimary)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = TextDisabled,
                            focusedLabelColor = GoldPrimary,
                            unfocusedLabelColor = TextSecondary,
                            cursorColor = GoldPrimary,
                            focusedContainerColor = DarkCard,
                            unfocusedContainerColor = DarkCard
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Telefon
                    OutlinedTextField(
                        value = phoneState.value,
                        onValueChange = { phoneState.value = it },
                        label = { Text("Telefon") },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, null, tint = GoldPrimary)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = TextDisabled,
                            focusedLabelColor = GoldPrimary,
                            unfocusedLabelColor = TextSecondary,
                            cursorColor = GoldPrimary,
                            focusedContainerColor = DarkCard,
                            unfocusedContainerColor = DarkCard
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            val errorMsg = localError.value ?: state.error
            if (!errorMsg.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = AccentRed.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = errorMsg,
                        modifier = Modifier.padding(12.dp),
                        color = AccentRed,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val showLoading = state.isLoading || isUploading.value
            Button(
                onClick = {
                    scope.launch {
                        localError.value = null
                        var profileUrl = ""

                        try {
                            if (imageUri.value != null) {
                                isUploading.value = true
                                Log.d(TAG, "Započinjem upload slike...")

                                val uploadResult = CloudinaryHelper.uploadImage(
                                    imageUri = imageUri.value!!,
                                    folder = "fitmap/profiles"
                                )

                                isUploading.value = false

                                if (uploadResult.isSuccess) {
                                    profileUrl = uploadResult.getOrNull() ?: ""
                                    Log.d(TAG, "Slika uspešno upload-ovana: $profileUrl")
                                } else {
                                    val error = uploadResult.exceptionOrNull()?.message ?: "Greška pri upload-u slike"
                                    Log.e(TAG, "Upload neuspešan: $error")
                                    localError.value = error
                                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                    return@launch
                                }
                            }

                            Log.d(TAG, "Započinjem registraciju korisnika...")
                            authViewModel.register(
                                emailState.value.trim(),
                                passwordState.value,
                                usernameState.value.trim(),
                                firstNameState.value.trim(),
                                lastNameState.value.trim(),
                                phoneState.value.trim(),
                                profileUrl
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
                enabled = !showLoading &&
                        emailState.value.isNotEmpty() &&
                        passwordState.value.isNotEmpty() &&
                        usernameState.value.isNotEmpty() &&
                        firstNameState.value.isNotEmpty() &&
                        lastNameState.value.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GoldPrimary,
                    contentColor = Color.Black,
                    disabledContainerColor = GoldPrimary.copy(alpha = 0.4f)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                if (showLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Registruj se",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Već imaš nalog? Prijavi se",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
