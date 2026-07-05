package veljko.najdanovic19273.fitmap.ui.map

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import veljko.najdanovic19273.fitmap.data.model.Comment
import veljko.najdanovic19273.fitmap.data.model.MapObject
import veljko.najdanovic19273.fitmap.viewmodel.MapViewModel
import veljko.najdanovic19273.fitmap.util.getObjectTypeName
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjectDetailsScreen(
    navController: NavController,
    mapViewModel: MapViewModel,
    objectId: String
) {
    var mapObject by remember { mutableStateOf<MapObject?>(null) }
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var userRating by remember { mutableStateOf<Float?>(null) }
    var commentText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var childObjects by remember { mutableStateOf<List<MapObject>>(emptyList()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    val currentUser = FirebaseAuth.getInstance().currentUser

    // Debug log - prikaži tačan objectId koji je primen
    LaunchedEffect(objectId) {
        android.util.Log.d("ObjectDetailsScreen", "═══════════════════════════════════════")
        android.util.Log.d("ObjectDetailsScreen", "Primljen objectId: '$objectId'")
        android.util.Log.d("ObjectDetailsScreen", "Dužina objectId: ${objectId.length}")
        android.util.Log.d("ObjectDetailsScreen", "═══════════════════════════════════════")
    }

    // NOVO: Real-time listener za detalje objekta, komentare i ocene
    DisposableEffect(objectId) {
        android.util.Log.d("ObjectDetailsScreen", "🔄 Pokrećem REAL-TIME listener za objekat: $objectId")

        val listener = mapViewModel.observeObjectDetails(objectId) { obj, cmts, rating ->
            android.util.Log.d("ObjectDetailsScreen", "🔄 REAL-TIME UPDATE primljen!")
            android.util.Log.d("ObjectDetailsScreen", "   - Objekat: ${obj?.title ?: "NULL"}")
            android.util.Log.d("ObjectDetailsScreen", "   - Komentari: ${cmts.size}")
            android.util.Log.d("ObjectDetailsScreen", "   - Ocena: ${rating ?: "nema"}")

            mapObject = obj
            comments = cmts
            userRating = rating
            isLoading = false
        }

        onDispose {
            android.util.Log.d("ObjectDetailsScreen", "🛑 Zaustavljam REAL-TIME listener")
            listener.remove()
        }
    }

    // NOVO: Real-time listener za child objekte (ako je teretana)
    DisposableEffect(objectId, mapObject?.type) {
        if (mapObject?.type == veljko.najdanovic19273.fitmap.data.model.ObjectType.GYM) {
            android.util.Log.d("ObjectDetailsScreen", "🔄 Pokrećem REAL-TIME listener za child objekte")

            val childListener = mapViewModel.observeChildObjects(objectId) { children ->
                android.util.Log.d("ObjectDetailsScreen", "🔄 REAL-TIME UPDATE - Child objekti: ${children.size}")
                childObjects = children
            }

            onDispose {
                android.util.Log.d("ObjectDetailsScreen", "🛑 Zaustavljam REAL-TIME listener za child objekte")
                childListener.remove()
            }
        } else {
            onDispose { }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalji objekta") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Nazad")
                    }
                },
                actions = {
                    // Dugme za brisanje - vidljivo samo autoru objekta
                    if (currentUser != null && mapObject?.authorId == currentUser.uid) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Obriši",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (mapObject == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Objekat nije pronađen")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Slika objekta
                item {
                    if (mapObject!!.imageUrl.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        ) {
                            AsyncImage(
                                model = mapObject!!.imageUrl,
                                contentDescription = "Slika objekta",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit, // FIT umesto CROP - prikazuje celu sliku
                                alignment = Alignment.Center
                            )
                        }
                    }
                }

                // Osnovne informacije
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = mapObject!!.title,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = getObjectTypeName(mapObject!!.type),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = mapObject!!.description,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Dodao: ${mapObject!!.authorName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Ocena
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text(
                                        text = "Prosečna ocena",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "%.1f".format(mapObject!!.averageRating),
                                            style = MaterialTheme.typography.headlineSmall
                                        )
                                        Icon(
                                            Icons.Filled.Star,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                        Text(
                                            text = "(${mapObject!!.ratingsCount})",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Ocenjivanje
                            if (currentUser != null) {
                                Text(
                                    text = "Vaša ocena:",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                RatingBar(
                                    rating = userRating ?: 0f,
                                    onRatingChanged = { newRating ->
                                        userRating = newRating
                                        mapViewModel.rateObject(
                                            objectId = objectId,
                                            authorId = currentUser.uid,
                                            rating = newRating
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                // Dodaj objekat u teretanu (samo za GYM tip)
                if (mapObject!!.type == veljko.najdanovic19273.fitmap.data.model.ObjectType.GYM && currentUser != null) {
                    item {
                        val gym = mapObject!! // Lokalna kopija za smart cast
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Button(
                                onClick = {
                                    navController.navigate(
                                        "${veljko.najdanovic19273.fitmap.ui.navigation.Screen.ADD_OBJECT}?lat=${gym.location.latitude.toFloat()}&lng=${gym.location.longitude.toFloat()}&gymId=${gym.id}"
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Dodaj spravu ili događaj u ovu teretanu")
                            }
                        }
                    }
                }

                // NOVO: Prikaz podređenih objekata (sprave, događaji, itd.)
                if (mapObject!!.type == veljko.najdanovic19273.fitmap.data.model.ObjectType.GYM && childObjects.isNotEmpty()) {
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                        Text(
                            text = "Sadržaj teretane (${childObjects.size})",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    items(childObjects) { childObject ->
                        ChildObjectItem(
                            childObject = childObject,
                            onClick = {
                                navController.navigate(
                                    veljko.najdanovic19273.fitmap.ui.navigation.Screen.objectDetails(childObject.id)
                                )
                            }
                        )
                    }
                }

                // Sekcija komentara
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    Text(
                        text = "Komentari (${comments.size})",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // Lista komentara
                items(comments) { comment ->
                    CommentItem(comment = comment)
                }

                // Dodaj komentar
                if (currentUser != null) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = commentText,
                                    onValueChange = { commentText = it },
                                    placeholder = { Text("Dodajte komentar...") },
                                    modifier = Modifier.weight(1f),
                                    maxLines = 3
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        if (commentText.isNotBlank()) {
                                            mapViewModel.addComment(
                                                objectId = objectId,
                                                authorId = currentUser.uid,
                                                text = commentText
                                            )
                                            commentText = ""
                                        }
                                    }
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Pošalji")
                                }
                            }
                        }
                    }
                }

                // Spacing na dnu
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Dialog za potvrdu brisanja
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                icon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text("Obriši ${if (mapObject?.type == veljko.najdanovic19273.fitmap.data.model.ObjectType.GYM) "teretanu" else "objekat"}?") },
                text = {
                    Column {
                        Text(
                            "Da li ste sigurni da želite da obrišete '${mapObject?.title}'?",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (mapObject?.type == veljko.najdanovic19273.fitmap.data.model.ObjectType.GYM && childObjects.isNotEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Text(
                                    "⚠️ Upozorenje: Ova teretana ima ${childObjects.size} podređenih objekata koji će takođe biti obrisani!",
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Ova akcija ne može biti poništena.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isDeleting = true
                            mapViewModel.deleteObject(
                                objectId = objectId,
                                onSuccess = {
                                    isDeleting = false
                                    showDeleteDialog = false
                                    navController.navigateUp()
                                },
                                onError = { error ->
                                    isDeleting = false
                                    showDeleteDialog = false
                                    // Možeš dodati Snackbar ili Toast za prikaz greške
                                    android.util.Log.e("ObjectDetailsScreen", "Greška pri brisanju: $error")
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        enabled = !isDeleting
                    ) {
                        if (isDeleting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onError,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (isDeleting) "Brišem..." else "Obriši")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteDialog = false },
                        enabled = !isDeleting
                    ) {
                        Text("Otkaži")
                    }
                }
            )
        }
    }
}

@Composable
fun RatingBar(
    rating: Float,
    onRatingChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        for (i in 1..5) {
            IconButton(onClick = { onRatingChanged(i.toFloat()) }) {
                Icon(
                    imageVector = if (i <= rating) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = "Star $i",
                    tint = if (i <= rating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun CommentItem(comment: Comment) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar - prikaži sliku ili inicijal
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    if (comment.authorImageUrl.isNotEmpty()) {
                        // Prikaži profilnu sliku
                        AsyncImage(
                            model = comment.authorImageUrl,
                            contentDescription = "Profilna slika ${comment.authorName}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Fallback na inicijal ako nema slike
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = comment.authorName.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = comment.authorName,
                        style = MaterialTheme.typography.titleSmall
                    )
                    comment.createdAt?.let { date ->
                        Text(
                            text = formatDate(date),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return formatter.format(date)
}

// NOVO: UI komponenta za prikaz podređenog objekta (sprava, događaj, itd.)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildObjectItem(
    childObject: MapObject,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ikonica prema tipu objekta
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = when (childObject.type) {
                            veljko.najdanovic19273.fitmap.data.model.ObjectType.EQUIPMENT -> "💪"
                            veljko.najdanovic19273.fitmap.data.model.ObjectType.FREE_EQUIPMENT -> "✅"
                            veljko.najdanovic19273.fitmap.data.model.ObjectType.CROWDED_AREA -> "👥"
                            veljko.najdanovic19273.fitmap.data.model.ObjectType.TRAINER_RECOMMENDATION -> "🎯"
                            veljko.najdanovic19273.fitmap.data.model.ObjectType.EVENT -> "📅"
                            else -> "📍"
                        },
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = childObject.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = getObjectTypeName(childObject.type),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (childObject.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = childObject.description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Ocena
            if (childObject.ratingsCount > 0) {
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "%.1f".format(childObject.averageRating),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
