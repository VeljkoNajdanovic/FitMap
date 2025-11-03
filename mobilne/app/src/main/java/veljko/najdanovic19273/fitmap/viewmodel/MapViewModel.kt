package veljko.najdanovic19273.fitmap.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import veljko.najdanovic19273.fitmap.data.model.MapObject
import veljko.najdanovic19273.fitmap.util.Constants

class MapViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    data class MapUiState(
        val isLoading: Boolean = false,
        val currentLocation: LatLng? = null,
        val mapObjects: List<MapObject> = emptyList(),
        val allMapObjects: List<MapObject> = emptyList(), // NOVO: Čuva sve objekte
        val selectedObject: MapObject? = null,
        val error: String? = null,
        val filterState: veljko.najdanovic19273.fitmap.data.model.FilterState = veljko.najdanovic19273.fitmap.data.model.FilterState() // NOVO: Filter state
    )

    private val _state = MutableStateFlow(MapUiState())
    val state: StateFlow<MapUiState> = _state.asStateFlow()

    companion object {
        private const val TAG = "MapViewModel"
    }

    init {
        loadMapObjects()
        // Postavi test lokaciju (Niš centar)
        setCurrentLocation(43.3209, 21.8958)
    }

    // Učitavanje svih objekata sa mape iz Firestore-a
    fun loadMapObjects() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                Log.d(TAG, "Učitavam objekte sa mape...")

                val snapshot = firestore.collection(Constants.OBJECTS_COLLECTION)
                    .get()
                    .await()

                val objects = snapshot.documents.mapNotNull { doc ->
                    // Koristi novu fromFirestore metodu koja garantovano postavlja ID
                    MapObject.fromFirestore(doc.id, doc.data ?: emptyMap())
                }

                Log.d(TAG, "Učitano ${objects.size} objekata")
                objects.forEach { obj ->
                    Log.d(TAG, "  - ${obj.title} (ID: ${obj.id})")
                }

                _state.value = _state.value.copy(
                    mapObjects = objects,
                    allMapObjects = objects, // Čuvanje svih objekata
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "Greška pri učitavanju objekata", e)
                _state.value = _state.value.copy(
                    error = e.message ?: "Greška pri učitavanju objekata",
                    isLoading = false
                )
            }
        }
    }

    // Postavljanje trenutne lokacije korisnika
    fun setCurrentLocation(latitude: Double, longitude: Double) {
        _state.value = _state.value.copy(
            currentLocation = LatLng(latitude, longitude)
        )
        Log.d(TAG, "Trenutna lokacija postavljena: $latitude, $longitude")
    }

    // Selekcija objekta na mapi
    fun selectObject(mapObject: MapObject) {
        _state.value = _state.value.copy(selectedObject = mapObject)
        Log.d(TAG, "Selektovan objekat: ${mapObject.title}")
    }

    // Deselekcija objekta
    fun deselectObject() {
        _state.value = _state.value.copy(selectedObject = null)
    }

    // Dodavanje novog objekta na mapu
    fun addMapObject(
        title: String,
        description: String,
        type: veljko.najdanovic19273.fitmap.data.model.ObjectType,
        latitude: Double,
        longitude: Double,
        authorId: String,
        authorName: String,
        imageUrl: String = "",
        attributes: Map<String, String> = emptyMap(),
        parentGymId: String? = null  // Dodato: ID roditeljske teretane
    ) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Dodajem novi objekat: $title")

                val newObject = MapObject(
                    title = title,
                    description = description,
                    type = type,
                    location = GeoPoint(latitude, longitude),
                    imageUrl = imageUrl,
                    authorId = authorId,
                    authorName = authorName,
                    attributes = attributes,
                    parentGymId = parentGymId
                )

                val docRef = firestore.collection(Constants.OBJECTS_COLLECTION)
                    .add(newObject.toMap())
                    .await()

                Log.d(TAG, "Objekat dodat sa ID: ${docRef.id}")

                // Dodaj poene korisniku
                addPointsToUser(authorId, Constants.POINTS_ADD_OBJECT)

                // Reload objects
                loadMapObjects()
            } catch (e: Exception) {
                Log.e(TAG, "Greška pri dodavanju objekta", e)
                _state.value = _state.value.copy(
                    error = e.message ?: "Greška pri dodavanju objekta"
                )
            }
        }
    }

    // Kalkulacija distance između dve tačke (Haversine formula)
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Radius zemlje u metrima
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    // Učitavanje detalja objekta sa komentarima i ocenama
    fun loadObjectDetails(
        objectId: String,
        onResult: (MapObject?, List<veljko.najdanovic19273.fitmap.data.model.Comment>, Float?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "═══════════════════════════════════════")
                Log.d(TAG, "Učitavam detalje za objekat: '$objectId'")
                Log.d(TAG, "═══════════════════════���═══════���═══════")

                // Učitaj objekat
                val objectDoc = firestore.collection(Constants.OBJECTS_COLLECTION)
                    .document(objectId)
                    .get()
                    .await()

                if (!objectDoc.exists()) {
                    Log.e(TAG, "❌ Objekat sa ID '$objectId' NE POSTOJI u bazi!")
                    onResult(null, emptyList(), null)
                    return@launch
                }

                // Koristi fromFirestore metodu koja garantovano postavlja ID
                val mapObject = MapObject.fromFirestore(objectDoc.id, objectDoc.data ?: emptyMap())

                if (mapObject == null) {
                    Log.e(TAG, "❌ Greška pri parsiranju objekta!")
                    onResult(null, emptyList(), null)
                    return@launch
                }

                Log.d(TAG, "✅ Objekat uspešno učitan:")
                Log.d(TAG, "   - ID: ${mapObject.id}")
                Log.d(TAG, "   - Naziv: ${mapObject.title}")
                Log.d(TAG, "   - Tip: ${mapObject.type}")

                // Učitaj komentare
                val commentsSnapshot = firestore.collection(Constants.COMMENTS_COLLECTION)
                    .whereEqualTo("objectId", objectId)
                    // Uklanjam orderBy jer zahteva Firestore indeks koji trenutno ne postoji
                    // .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()

                val comments = commentsSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(veljko.najdanovic19273.fitmap.data.model.Comment::class.java)?.copy(id = doc.id)
                }.sortedByDescending { it.createdAt } // Sortiraj na klijentskoj strani

                Log.d(TAG, "✅ Učitano ${comments.size} komentara")

                // Učitaj korisničku ocenu
                val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                var userRating: Float? = null

                if (currentUserId != null) {
                    val ratingSnapshot = firestore.collection(Constants.RATINGS_COLLECTION)
                        .whereEqualTo("objectId", objectId)
                        .whereEqualTo("authorId", currentUserId)
                        .get()
                        .await()

                    userRating = ratingSnapshot.documents.firstOrNull()
                        ?.toObject(veljko.najdanovic19273.fitmap.data.model.Rating::class.java)
                        ?.value

                    Log.d(TAG, "✅ Korisnička ocena: ${userRating ?: "nema"}")
                }

                onResult(mapObject, comments, userRating)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Greška pri učitavanju detalja objekta: ${e.message}", e)
                onResult(null, emptyList(), null)
            }
        }
    }

    // Dodavanje ocene objektu
    fun rateObject(objectId: String, authorId: String, rating: Float) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Dodajem ocenu $rating za objekat $objectId")

                // Proveri da li korisnik već ima ocenu
                val existingRating = firestore.collection(Constants.RATINGS_COLLECTION)
                    .whereEqualTo("objectId", objectId)
                    .whereEqualTo("authorId", authorId)
                    .get()
                    .await()

                val ratingData = veljko.najdanovic19273.fitmap.data.model.Rating(
                    objectId = objectId,
                    authorId = authorId,
                    value = rating
                )

                if (existingRating.documents.isNotEmpty()) {
                    // Update postojeće ocene
                    existingRating.documents.first().reference
                        .update("value", rating)
                        .await()
                } else {
                    // Dodaj novu ocenu
                    firestore.collection(Constants.RATINGS_COLLECTION)
                        .add(ratingData.toMap())
                        .await()
                }

                // Ažuriraj prosečnu ocenu objekta
                updateObjectAverageRating(objectId)

                Log.d(TAG, "Ocena uspešno dodata")
            } catch (e: Exception) {
                Log.e(TAG, "Greška pri dodavanju ocene", e)
            }
        }
    }

    // Ažuriranje prosečne ocene objekta
    private suspend fun updateObjectAverageRating(objectId: String) {
        try {
            val ratingsSnapshot = firestore.collection(Constants.RATINGS_COLLECTION)
                .whereEqualTo("objectId", objectId)
                .get()
                .await()

            val ratings = ratingsSnapshot.documents.mapNotNull { doc ->
                doc.toObject(veljko.najdanovic19273.fitmap.data.model.Rating::class.java)
            }

            val averageRating = if (ratings.isNotEmpty()) {
                ratings.map { it.value }.average()
            } else {
                0.0
            }

            firestore.collection(Constants.OBJECTS_COLLECTION)
                .document(objectId)
                .update(
                    mapOf(
                        "averageRating" to averageRating,
                        "ratingsCount" to ratings.size
                    )
                )
                .await()

            Log.d(TAG, "Prosečna ocena ažurirana: $averageRating")
        } catch (e: Exception) {
            Log.e(TAG, "Greška pri ažuriranju prosečne ocene", e)
        }
    }

    // Dodavanje komentara objektu
    fun addComment(
        objectId: String,
        authorId: String,
        authorName: String,
        authorImageUrl: String,
        text: String
    ) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Dodajem komentar za objekat $objectId")

                val comment = veljko.najdanovic19273.fitmap.data.model.Comment(
                    objectId = objectId,
                    authorId = authorId,
                    authorName = authorName,
                    authorImageUrl = authorImageUrl,
                    text = text
                )

                firestore.collection(Constants.COMMENTS_COLLECTION)
                    .add(comment.toMap())
                    .await()

                // Ažuriranje broja komentara
                firestore.collection(Constants.OBJECTS_COLLECTION)
                    .document(objectId)
                    .update("commentsCount", com.google.firebase.firestore.FieldValue.increment(1))
                    .await()

                // Dodaj poene korisniku
                addPointsToUser(authorId, Constants.POINTS_ADD_COMMENT)

                Log.d(TAG, "Komentar uspešno dodat")
            } catch (e: Exception) {
                Log.e(TAG, "Greška pri dodavanju komentara", e)
            }
        }
    }

    // NOVO: Učitavanje podređenih objekata (sprave, događaji, itd.) za teretanu
    fun loadChildObjects(parentGymId: String, onResult: (List<MapObject>) -> Unit) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Učitavam podređene objekte za teretanu: $parentGymId")

                val snapshot = firestore.collection(Constants.OBJECTS_COLLECTION)
                    .whereEqualTo("parentGymId", parentGymId)
                    .get()
                    .await()

                val childObjects = snapshot.documents.mapNotNull { doc ->
                    MapObject.fromFirestore(doc.id, doc.data ?: emptyMap())
                }

                Log.d(TAG, "Učitano ${childObjects.size} podređenih objekata")
                childObjects.forEach { obj ->
                    Log.d(TAG, "  - ${obj.title} (${obj.type})")
                }

                onResult(childObjects)
            } catch (e: Exception) {
                Log.e(TAG, "Greška pri učitavanju podređenih objekata", e)
                onResult(emptyList())
            }
        }
    }

    // Dodavanje poena korisniku
    private fun addPointsToUser(userId: String, points: Int) {
        viewModelScope.launch {
            try {
                firestore.collection(Constants.USERS_COLLECTION)
                    .document(userId)
                    .update("points", com.google.firebase.firestore.FieldValue.increment(points.toLong()))
                    .await()
                Log.d(TAG, "Dodato $points poena korisniku $userId")
            } catch (e: Exception) {
                Log.e(TAG, "Greška pri dodavanju poena", e)
            }
        }
    }

    // NOVO: Brisanje objekta sa mape
    fun deleteObject(
        objectId: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "═══════════════════════���═══════════════")
                Log.d(TAG, "🗑️ BRISANJE OBJEKTA: $objectId")

                // Prvo obriši sve podređene objekte (ako postoje)
                val childrenSnapshot = firestore.collection(Constants.OBJECTS_COLLECTION)
                    .whereEqualTo("parentGymId", objectId)
                    .get()
                    .await()

                Log.d(TAG, "Pronađeno ${childrenSnapshot.size()} podređenih objekata za brisanje")

                // Obriši sve podređene objekte
                for (childDoc in childrenSnapshot.documents) {
                    childDoc.reference.delete().await()
                    Log.d(TAG, "  ✓ Obrisan podređeni objekat: ${childDoc.id}")
                }

                // Obriši sve komentare vezane za ovaj objekat
                val commentsSnapshot = firestore.collection(Constants.COMMENTS_COLLECTION)
                    .whereEqualTo("objectId", objectId)
                    .get()
                    .await()

                Log.d(TAG, "Pronađeno ${commentsSnapshot.size()} komentara za brisanje")

                for (commentDoc in commentsSnapshot.documents) {
                    commentDoc.reference.delete().await()
                }

                // Obriši sve ocene vezane za ovaj objekat
                val ratingsSnapshot = firestore.collection(Constants.RATINGS_COLLECTION)
                    .whereEqualTo("objectId", objectId)
                    .get()
                    .await()

                Log.d(TAG, "Pronađeno ${ratingsSnapshot.size()} ocena za brisanje")

                for (ratingDoc in ratingsSnapshot.documents) {
                    ratingDoc.reference.delete().await()
                }

                // Na kraju obriši sam objekat
                firestore.collection(Constants.OBJECTS_COLLECTION)
                    .document(objectId)
                    .delete()
                    .await()

                Log.d(TAG, "✅ OBJEKAT USPEŠNO OBRISAN!")
                Log.d(TAG, "═══════════════════════���═══════════════")

                // Reload objekata
                loadMapObjects()

                onSuccess()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Greška pri brisanju objekta: ${e.message}", e)
                onError(e.message ?: "Nepoznata greška")
            }
        }
    }

    // NOVO: Primena kompletnog filtera na objekte
    fun applyFilter(filter: veljko.najdanovic19273.fitmap.data.model.FilterState) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "═══════════════════════════════════════")
                Log.d(TAG, "🔍 PRIMENA FILTERA:")
                Log.d(TAG, "   Search: '${filter.searchQuery}'")
                Log.d(TAG, "   Tip: ${filter.selectedType?.name ?: "Svi"}")
                Log.d(TAG, "   Radijus: ${filter.radiusInMeters?.let { "${it}m" } ?: "Sve"}")
                Log.d(TAG, "   Min ocena: ${if (filter.minRating > 0) "${filter.minRating}⭐" else "Sve"}")
                Log.d(TAG, "═══════════════════════════════════════")

                var filteredObjects = _state.value.allMapObjects

                // 1. Filter po pretrazi (naziv ili opis)
                if (filter.searchQuery.isNotBlank()) {
                    filteredObjects = filteredObjects.filter { obj ->
                        obj.title.contains(filter.searchQuery, ignoreCase = true) ||
                        obj.description.contains(filter.searchQuery, ignoreCase = true)
                    }
                    Log.d(TAG, "   ✓ Posle pretrage: ${filteredObjects.size} objekata")

                    // NOVO: Dodaj roditeljske teretane za pronađene child objekte
                    val parentGymIds = filteredObjects
                        .filter { it.parentGymId != null }
                        .mapNotNull { it.parentGymId }
                        .distinct()

                    val parentGyms = _state.value.allMapObjects.filter { gym ->
                        gym.type == veljko.najdanovic19273.fitmap.data.model.ObjectType.GYM &&
                        parentGymIds.contains(gym.id)
                    }

                    // Kombinuj pronađene objekte sa njihovim roditeljskim teretanama
                    filteredObjects = (filteredObjects + parentGyms).distinctBy { it.id }

                    Log.d(TAG, "   ✓ Dodato ${parentGyms.size} roditeljskih teretana")
                    Log.d(TAG, "   ✓ Ukupno objekata sa roditeljima: ${filteredObjects.size}")
                }

                // 2. Filter po tipu objekta
                if (filter.selectedType != null) {
                    filteredObjects = filteredObjects.filter { obj ->
                        obj.type == filter.selectedType
                    }
                    Log.d(TAG, "   ✓ Posle filtera po tipu: ${filteredObjects.size} objekata")
                }

                // 3. Filter po radijusu (ako je postavljen i ako postoji trenutna lokacija)
                if (filter.radiusInMeters != null && _state.value.currentLocation != null) {
                    val currentLoc = _state.value.currentLocation!!
                    filteredObjects = filteredObjects.filter { obj ->
                        val distance = calculateDistance(
                            currentLoc.latitude, currentLoc.longitude,
                            obj.location.latitude, obj.location.longitude
                        )
                        distance <= filter.radiusInMeters
                    }
                    Log.d(TAG, "   ✓ Posle filtera po radijusu: ${filteredObjects.size} objekata")
                }

                // 4. Filter po minimalnoj oceni
                if (filter.minRating > 0) {
                    filteredObjects = filteredObjects.filter { obj ->
                        obj.averageRating >= filter.minRating
                    }
                    Log.d(TAG, "   ✓ Posle filtera po oceni: ${filteredObjects.size} objekata")
                }

                Log.d(TAG, "═══════════════════════════════════════")
                Log.d(TAG, "✅ UKUPNO FILTRIRANIH OBJEKATA: ${filteredObjects.size}")
                val gymCount = filteredObjects.filter { it.type == veljko.najdanovic19273.fitmap.data.model.ObjectType.GYM }.size
                Log.d(TAG, "✅ OD TOGA TERETANA: $gymCount")
                Log.d(TAG, "═══════════════════════════════════════")

                _state.value = _state.value.copy(
                    mapObjects = filteredObjects,
                    filterState = filter
                )
            } catch (e: Exception) {
                Log.e(TAG, "Greška pri primeni filtera", e)
            }
        }
    }

    // NOVO: Resetovanje filtera
    fun resetFilter() {
        _state.value = _state.value.copy(
            mapObjects = _state.value.allMapObjects,
            filterState = veljko.najdanovic19273.fitmap.data.model.FilterState()
        )
    }
}
