package veljko.najdanovic19273.fitmap.ui.map

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import veljko.najdanovic19273.fitmap.data.model.ObjectType
import veljko.najdanovic19273.fitmap.util.getObjectTypeName
import veljko.najdanovic19273.fitmap.util.CloudinaryHelper
import veljko.najdanovic19273.fitmap.viewmodel.MapViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AddObjectScreen(
    navController: NavController,
    mapViewModel: MapViewModel,
    latitude: Double,
    longitude: Double,
    parentGymId: String? = null
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(if (parentGymId != null) ObjectType.EQUIPMENT else ObjectType.GYM) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    var showTypeDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var selectedQuality by remember { mutableStateOf(ImageQuality.MEDIUM) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var markerPosition by remember { mutableStateOf(LatLng(latitude, longitude)) }
    var isMapLoaded by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            imageUri = uri
            showQualityDialog = true // Prikaži dijalog za izbor kvaliteta
        }
    }

    val currentUser = FirebaseAuth.getInstance().currentUser

    // Camera state za mapu
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(markerPosition, 15f)
    }

    // Marker state - zapamtiti poziciju
    val markerState = rememberMarkerState(position = markerPosition)

    // Sinhronizuj marker sa promenom pozicije
    LaunchedEffect(markerPosition) {
        markerState.position = markerPosition
        Log.d("AddObjectScreen", "Marker pozicija promenjena: ${markerPosition.latitude}, ${markerPosition.longitude}")
    }

    // Debug log za inicijalizaciju
    LaunchedEffect(Unit) {
        Log.d("AddObjectScreen", "Inicijalizacija mape sa pozicijom: lat=$latitude, lng=$longitude")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (parentGymId != null)
                            "Dodaj opremu/događaj"
                        else
                            "Dodaj novu teretanu"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Nazad")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Info poruka na vrhu
            if (parentGymId != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ℹ️",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column {
                            Text(
                                text = "Dodaješ opremu u teretanu",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Lokacija će biti ista kao kod teretane",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // MAPA SA MARKEROM - samo za teretane (uvećana i na vrhu ekrana)
            if (parentGymId == null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            properties = MapProperties(
                                mapType = MapType.NORMAL,
                                isMyLocationEnabled = false
                            ),
                            uiSettings = MapUiSettings(
                                zoomControlsEnabled = true,
                                compassEnabled = true,
                                rotationGesturesEnabled = true,
                                tiltGesturesEnabled = false,
                                scrollGesturesEnabled = true,
                                zoomGesturesEnabled = true,
                                myLocationButtonEnabled = false
                            ),
                            onMapClick = { latLng ->
                                // Omogućava i klik na mapi da pomeri marker
                                markerPosition = latLng
                                Log.d("AddObjectScreen", "Kliknuto na mapu: ${latLng.latitude}, ${latLng.longitude}")
                            },
                            onMapLoaded = {
                                isMapLoaded = true
                                Log.d("AddObjectScreen", "Mapa uspešno učitana!")
                            }
                        ) {
                            // Marker na poziciji
                            Marker(
                                state = markerState,
                                title = "📍 Nova teretana",
                                snippet = "Klikni na mapu ili prevuci marker",
                                draggable = true,
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                                onInfoWindowClick = {
                                    Log.d("AddObjectScreen", "Info window kliknut")
                                }
                            )
                        }

                        // Ako mapa nije učitana, prikaži loading indicator
                        if (!isMapLoaded) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        // Info overlay
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small,
                            shadowElevation = 4.dp
                        ) {
                            Text(
                                text = if (!isMapLoaded)
                                    "⏳ Učitavam mapu..."
                                else
                                    "💡 Klikni na mapu ili prevuci marker 🔴",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Naslov
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(if (parentGymId != null) "Naziv objekta" else "Naziv teretane") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(if (parentGymId != null) "npr. Bench press, Traka za trčanje..." else "npr. Gym 011, Iron Paradise...") }
            )

            // Opis
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Opis") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5,
                placeholder = { Text("Opišite detalje...") }
            )

            // Tip objekta - SAMO ako dodajemo u teretanu
            if (parentGymId != null) {
                OutlinedButton(
                    onClick = { showTypeDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tip: ${getObjectTypeName(selectedType)}")
                }
            } else {
                // Prikaz da je ovo TERETANA
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tip objekta: ${getObjectTypeName(ObjectType.GYM)}",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            // Slika
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Slika objekta",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit, // Prikazuje celu sliku bez sečenja
                            alignment = Alignment.Center
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Dodaj sliku (opciono)")
                        }
                    }

                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    ) {
                        Text("Izaberi")
                    }
                }
            }

            // Lokacija info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "📍 Lokacija:",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        "Lat: %.6f, Lng: %.6f".format(markerPosition.latitude, markerPosition.longitude),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Error message
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Dugme za dodavanje
            Button(
                onClick = {
                    if (title.isBlank()) {
                        errorMessage = "Unesite naziv"
                        return@Button
                    }
                    if (currentUser == null) {
                        errorMessage = "Niste prijavljeni"
                        return@Button
                    }

                    isUploading = true
                    errorMessage = null

                    coroutineScope.launch {
                        try {
                            // Upload slike ako postoji
                            val imageUrl = if (imageUri != null) {
                                Log.d("AddObjectScreen", "Uplodujem sliku kvaliteta: ${selectedQuality.displayName}")
                                uploadProgress = 0.3f

                                val result = CloudinaryHelper.uploadImage(
                                    imageUri = imageUri!!,
                                    folder = "fitmap/objects"
                                )

                                uploadProgress = 0.7f

                                result.getOrElse {
                                    errorMessage = "Greška pri upload-u slike: ${it.message}"
                                    isUploading = false
                                    uploadProgress = 0f
                                    return@launch
                                }
                            } else {
                                ""
                            }

                            uploadProgress = 0.9f

                            // Dodaj objekat u Firestore
                            mapViewModel.addMapObject(
                                title = title,
                                description = description,
                                type = selectedType,
                                latitude = markerPosition.latitude,
                                longitude = markerPosition.longitude,
                                authorId = currentUser.uid,
                                authorName = currentUser.displayName ?: "Nepoznat korisnik",
                                imageUrl = imageUrl,
                                parentGymId = parentGymId
                            )

                            uploadProgress = 1f
                            Log.d("AddObjectScreen", "Objekat uspešno dodat sa slikom: $imageUrl")

                            // Vrati se nazad
                            navController.navigateUp()
                        } catch (e: Exception) {
                            Log.e("AddObjectScreen", "Greška pri dodavanju objekta", e)
                            errorMessage = "Greška: ${e.message}"
                            isUploading = false
                            uploadProgress = 0f
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isUploading
            ) {
                if (isUploading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Text("Uplodujem... ${(uploadProgress * 100).toInt()}%")
                    }
                } else {
                    Text(if (parentGymId != null) "Dodaj objekat" else "Dodaj teretanu")
                }
            }
        }

        // Dialog za izbor tipa - SAMO za objekte u teretani
        if (showTypeDialog && parentGymId != null) {
            AlertDialog(
                onDismissRequest = { showTypeDialog = false },
                title = { Text("Izaberite tip objekta") },
                text = {
                    Column {
                        // Prikazujemo SVE tipove OSIM GYM
                        ObjectType.entries.filter { it != ObjectType.GYM }.forEach { type ->
                            TextButton(
                                onClick = {
                                    selectedType = type
                                    showTypeDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = getObjectTypeName(type),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTypeDialog = false }) {
                        Text("Otkaži")
                    }
                }
            )
        }

        // Dialog za izbor kvaliteta slike
        if (showQualityDialog && imageUri != null) {
            AlertDialog(
                onDismissRequest = { showQualityDialog = false },
                title = { Text("Izaberite kvalitet slike") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Veći kvalitet = bolja slika, ali sporiji upload",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        ImageQuality.values().forEach { quality ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedQuality = quality
                                        showQualityDialog = false
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedQuality == quality)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = quality.displayName,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = quality.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (selectedQuality == quality) {
                                        Text(
                                            "✓",
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showQualityDialog = false }) {
                        Text("Zatvori")
                    }
                }
            )
        }
    }
}

enum class ImageQuality(val displayName: String, val description: String) {
    LOW("Nizak", "Brži upload, manja slika"),
    MEDIUM("Srednji", "Balans između kvaliteta i brzine upload-a"),
    HIGH("Visok", "Najbolji kvalitet, sporiji upload")
}
