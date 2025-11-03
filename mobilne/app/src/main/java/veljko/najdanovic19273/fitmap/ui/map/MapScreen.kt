package veljko.najdanovic19273.fitmap.ui.map

import android.Manifest
import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import veljko.najdanovic19273.fitmap.ui.navigation.Screen
import veljko.najdanovic19273.fitmap.viewmodel.MapViewModel
import veljko.najdanovic19273.fitmap.viewmodel.LocationViewModel

private const val TAG = "MapScreen"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    navController: NavController,
    mapViewModel: MapViewModel = viewModel(),
    locationViewModel: LocationViewModel = viewModel()
) {
    val state by mapViewModel.state.collectAsState()
    val locationServiceState by locationViewModel.state.collectAsState()

    // NOVO: State za filter bottom sheet
    var showFilterSheet by remember { mutableStateOf(false) }

    // NOVO: State za prikaz liste rezultata pretrage
    var showSearchResults by remember { mutableStateOf(false) }

    // NOVO: Automatski prikaži listu kada se primeni filter
    LaunchedEffect(state.filterState.isActive()) {
        if (state.filterState.isActive() && state.filterState.searchQuery.isNotBlank()) {
            showSearchResults = true
        }
    }

    // Location dozvole
    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // Notification dozvola (Android 13+)
    val notificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberMultiplePermissionsState(
            permissions = listOf(Manifest.permission.POST_NOTIFICATIONS)
        )
    } else {
        null
    }

    // Request permissions on first load
    LaunchedEffect(Unit) {
        if (!locationPermissions.allPermissionsGranted) {
            Log.d(TAG, "Tražim location dozvole...")
            locationPermissions.launchMultiplePermissionRequest()
        }
    }

    // Traži notification dozvolu nakon location dozvola
    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        if (locationPermissions.allPermissionsGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermission?.let { notifPerm ->
                    if (!notifPerm.allPermissionsGranted) {
                        Log.d(TAG, "Tražim notification dozvolu...")
                        notifPerm.launchMultiplePermissionRequest()
                    }
                }
            }
        }
    }

    // Automatski pokreni location tracking kad su sve dozvole date
    LaunchedEffect(locationPermissions.allPermissionsGranted, notificationPermission?.allPermissionsGranted) {
        val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission?.allPermissionsGranted ?: false
        } else {
            true // Ispod Android 13 ne treba dozvola
        }

        if (locationPermissions.allPermissionsGranted && notificationsGranted && !locationServiceState.isServiceRunning) {
            Log.d(TAG, "Sve dozvole date, pokrećem tracking...")
            locationViewModel.startLocationTracking()
        }
    }

    // Reload objects svaki put kada se ekran prikaže
    LaunchedEffect(navController) {
        val currentBackStackEntry = navController.currentBackStackEntry
        currentBackStackEntry?.savedStateHandle?.getLiveData<Boolean>("refresh")?.observeForever { refresh ->
            if (refresh == true) {
                mapViewModel.loadMapObjects()
                currentBackStackEntry.savedStateHandle.set("refresh", false)
            }
        }
        // Učitaj objekte pri prvom učitavanju
        Log.d(TAG, "Učitavam objekte sa mape...")
        mapViewModel.loadMapObjects()
    }

    // Debug - loguj broj učitanih objekata
    LaunchedEffect(state.mapObjects) {
        Log.d(TAG, "═══════════════════════════════════════")
        Log.d(TAG, "UKUPNO OBJEKATA: ${state.mapObjects.size}")
        state.mapObjects.forEach { obj ->
            Log.d(TAG, "  - ${obj.title} (${obj.type}) @ ${obj.location.latitude}, ${obj.location.longitude}")
        }
        val gymCount = state.mapObjects.filter { it.type == veljko.najdanovic19273.fitmap.data.model.ObjectType.GYM }.size
        Log.d(TAG, "TERETANA (GYM) objekata: $gymCount")
        Log.d(TAG, "═══════════════════════════════���═══════")
    }

    // Camera position state
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            state.currentLocation ?: LatLng(43.3209, 21.8958), // Default: Niš, Srbija
            15f
        )
    }

    // Update camera when current location changes
    LaunchedEffect(state.currentLocation) {
        state.currentLocation?.let { location ->
            cameraPositionState.position = CameraPosition.fromLatLngZoom(location, 15f)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("FitMap - Klikni na mapu")
                        // NOVO: Prikaz broja filtriranih objekata
                        if (state.filterState.isActive()) {
                            Text(
                                text = "${state.mapObjects.size} objekata",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { /* TODO: Open drawer */ }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    // NOVO: Filter dugme sa badge ako je filter aktivan
                    BadgedBox(
                        badge = {
                            if (state.filterState.isActive()) {
                                Badge { Text("●") }
                            }
                        }
                    ) {
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Filteri",
                                tint = if (state.filterState.isActive())
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Toggle za location tracking
                    IconButton(
                        onClick = {
                            if (locationServiceState.isServiceRunning) {
                                locationViewModel.stopLocationTracking()
                            } else {
                                if (locationPermissions.allPermissionsGranted) {
                                    locationViewModel.startLocationTracking()
                                } else {
                                    locationPermissions.launchMultiplePermissionRequest()
                                }
                            }
                        }
                    ) {
                        Icon(
                            if (locationServiceState.isServiceRunning)
                                Icons.Default.Notifications
                            else
                                Icons.Outlined.Notifications,
                            contentDescription = "Notifikacije",
                            tint = if (locationServiceState.isServiceRunning)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { navController.navigate(Screen.PROFILE) }) {
                        Icon(Icons.Default.Menu, contentDescription = "Profil")
                    }
                }
            )
        },
        floatingActionButton = {
            // FAB za centriranje na trenutnu lokaciju
            FloatingActionButton(
                onClick = {
                    state.currentLocation?.let { location ->
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(location, 15f)
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = "Moja lokacija")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!locationPermissions.allPermissionsGranted) {
                // Prikaz poruke za dozvolu
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "FitMap treba dozvolu za pristup lokaciji",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ovo omogućava prikazivanje vaše trenutne lokacije i objekata u blizini",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { locationPermissions.launchMultiplePermissionRequest() }) {
                        Text("Dozvoli pristup lokaciji")
                    }
                }
            } else {
                // Google Maps prikaz
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = true,
                        mapType = MapType.NORMAL
                    ),
                    uiSettings = MapUiSettings(
                        myLocationButtonEnabled = false,
                        zoomControlsEnabled = true,
                        compassEnabled = true
                    ),
                    onMapClick = { latLng ->
                        // OBICAN KLIK na mapu - odmah idi na dodavanje teretane
                        navController.navigate(
                            "${Screen.ADD_OBJECT}?lat=${latLng.latitude.toFloat()}&lng=${latLng.longitude.toFloat()}"
                        )
                    }
                ) {
                    // Markeri za objekte (samo teretane)
                    state.mapObjects.filter { it.type == veljko.najdanovic19273.fitmap.data.model.ObjectType.GYM }.forEach { mapObject ->
                        Marker(
                            state = MarkerState(
                                position = LatLng(
                                    mapObject.location.latitude,
                                    mapObject.location.longitude
                                )
                            ),
                            title = mapObject.title,
                            snippet = mapObject.description,
                            onClick = {
                                mapViewModel.selectObject(mapObject)
                                true
                            }
                        )
                    }
                }

                // Loading indicator
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(48.dp)
                    )
                }

                // Info card na dnu
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "👆 Klikni bilo gde na mapi da dodaš novu teretanu",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // NOVO: Dugme za uklanjanje rezultata pretrage - prikazuje se iznad info card-a
                if (state.filterState.isActive()) {
                    Button(
                        onClick = {
                            mapViewModel.resetFilter()
                            showSearchResults = false // Resetuj i prikaz liste
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 90.dp) // Iznad info card-a
                            .fillMaxWidth(0.9f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ukloni rezultate pretrage (${state.mapObjects.size})")
                    }
                }

                // Bottom sheet za detalje objekta
                state.selectedObject?.let { selectedObject ->
                    ObjectDetailsBottomSheet(
                        mapObject = selectedObject,
                        onDismiss = { mapViewModel.deselectObject() },
                        onNavigate = {
                            navController.navigate(Screen.objectDetails(selectedObject.id))
                            mapViewModel.deselectObject()
                        }
                    )
                }
            }
        }

        // NOVO: Bottom sheet za filtere
        if (showFilterSheet) {
            FilterBottomSheet(
                currentFilter = state.filterState,
                onFilterChanged = { newFilter ->
                    mapViewModel.applyFilter(newFilter)
                    // NE postavljaj showSearchResults ovde
                },
                onDismiss = {
                    showFilterSheet = false
                }
            )
        }

        // NOVO: Lista rezultata pretrage - prikazuje se samo kada je showSearchResults true
        if (showSearchResults && state.filterState.isActive() && state.mapObjects.isNotEmpty()) {
            SearchResultsList(
                objects = state.mapObjects,
                onObjectClick = { mapObject ->
                    // Zatvori listu rezultata
                    showSearchResults = false

                    // Centriraj kameru na objekat
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(
                        LatLng(mapObject.location.latitude, mapObject.location.longitude),
                        17f
                    )
                    // Selektuj objekat da se prikaže bottom sheet
                    mapViewModel.selectObject(mapObject)
                },
                onDismiss = {
                    // Kada se zatvori lista, SAMO sakrij listu - NE resetuj filter
                    showSearchResults = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjectDetailsBottomSheet(
    mapObject: veljko.najdanovic19273.fitmap.data.model.MapObject,
    onDismiss: () -> Unit,
    onNavigate: () -> Unit
) {
    // Debug log
    Log.d("ObjectDetailsBottomSheet", "Objekat: ${mapObject.title}, ID: '${mapObject.id}'")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = mapObject.title,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = mapObject.type.name.replace("_", " "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = mapObject.description,
                style = MaterialTheme.typography.bodyMedium
            )

            // Debug info - prikazuje ID objekta
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "ID: ${mapObject.id}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Ocena: %.1f⭐".format(java.util.Locale.US, mapObject.averageRating),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Komentari: ${mapObject.commentsCount}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(onClick = onNavigate) {
                    Text("Vidi detalje")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// NOVO: Komponenta za prikaz liste rezultata pretrage
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultsList(
    objects: List<veljko.najdanovic19273.fitmap.data.model.MapObject>,
    onObjectClick: (veljko.najdanovic19273.fitmap.data.model.MapObject) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f) // 70% visine ekrana
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rezultati pretrage",
                    style = MaterialTheme.typography.titleLarge
                )
                TextButton(onClick = onDismiss) {
                    Text("Zatvori")
                }
            }

            Text(
                text = "Pronađeno: ${objects.size} ${if (objects.size == 1) "objekat" else "objekata"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // GRUPISANJE: Prvo teretane, pa ostali objekti grupisani po teretani
            val gyms = objects.filter { it.type == veljko.najdanovic19273.fitmap.data.model.ObjectType.GYM }
            val childObjects = objects.filter { it.type != veljko.najdanovic19273.fitmap.data.model.ObjectType.GYM }

            // Grupisanje podređenih objekata po teretani
            val objectsByGym = childObjects.groupBy { it.parentGymId }

            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Prikaz teretana
                if (gyms.isNotEmpty()) {
                    item {
                        Text(
                            text = "🏋️ Teretane (${gyms.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(gyms.size) { index ->
                        val gym = gyms[index]
                        SearchResultCard(
                            mapObject = gym,
                            onClick = { onObjectClick(gym) },
                            showParentGym = false
                        )
                    }
                }

                // Prikaz podređenih objekata grupisanih po teretani
                objectsByGym.forEach { (gymId, childObjectsList) ->
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        // Pronađi teretanu
                        val parentGym = if (gymId != null) {
                            gyms.find { it.id == gymId } ?: objects.find { it.id == gymId }
                        } else null

                        Text(
                            text = if (parentGym != null)
                                "📍 U teretani: ${parentGym.title} (${childObjectsList.size})"
                            else
                                "📍 Objekti (${childObjectsList.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(childObjectsList.size) { index ->
                        val childObject = childObjectsList[index]
                        SearchResultCard(
                            mapObject = childObject,
                            onClick = { onObjectClick(childObject) },
                            showParentGym = false,
                            isChild = true
                        )
                    }
                }
            }
        }
    }
}

// NOVO: Kartica za jedan rezultat pretrage
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultCard(
    mapObject: veljko.najdanovic19273.fitmap.data.model.MapObject,
    onClick: () -> Unit,
    @Suppress("UNUSED_PARAMETER") showParentGym: Boolean = true,
    isChild: Boolean = false
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (isChild) 16.dp else 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isChild)
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji ikonica prema tipu
            Text(
                text = when (mapObject.type) {
                    veljko.najdanovic19273.fitmap.data.model.ObjectType.GYM -> "🏋️"
                    veljko.najdanovic19273.fitmap.data.model.ObjectType.EQUIPMENT -> "💪"
                    veljko.najdanovic19273.fitmap.data.model.ObjectType.FREE_EQUIPMENT -> "✅"
                    veljko.najdanovic19273.fitmap.data.model.ObjectType.CROWDED_AREA -> "👥"
                    veljko.najdanovic19273.fitmap.data.model.ObjectType.TRAINER_RECOMMENDATION -> "🎯"
                    veljko.najdanovic19273.fitmap.data.model.ObjectType.EVENT -> "📅"
                },
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(end = 12.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mapObject.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = veljko.najdanovic19273.fitmap.util.getObjectTypeName(mapObject.type),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (mapObject.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = mapObject.description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Ocena ako postoji
            if (mapObject.ratingsCount > 0) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "%.1f".format(mapObject.averageRating),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
