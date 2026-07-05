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
        val allMapObjects: List<MapObject> = emptyList(),
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
        text: String
    ) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Dodajem komentar za objekat $objectId")

                // Prvo učitaj korisničke podatke iz Firestore
                val userSnapshot = firestore.collection(Constants.USERS_COLLECTION)
                    .document(authorId)
                    .get()
                    .await()

                val username = userSnapshot.getString("username") ?: "Nepoznat korisnik"
                val profileImageUrl = userSnapshot.getString("profileImageUrl") ?: ""

                val comment = veljko.najdanovic19273.fitmap.data.model.Comment(
                    objectId = objectId,
                    authorId = authorId,
                    authorName = username,
                    authorImageUrl = profileImageUrl,
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
                Log.d(TAG, "   Trenutna lokacija: ${_state.value.currentLocation}")
                Log.d(TAG, "═══════════════════════════════════════")

                // VAŽNO: Proveri da li postoji trenutna lokacija ako je potreban radius filter
                if (filter.radiusInMeters != null && _state.value.currentLocation == null) {
                    Log.e(TAG, "❌ GREŠKA: Radijus filter je postavljen ali NEMA trenutne lokacije!")
                    Log.e(TAG, "❌ Filter po radijusu NEĆE raditi dok se ne dobije GPS lokacija!")
                    _state.value = _state.value.copy(
                        error = "Čekam GPS lokaciju... Filter po radijusu će raditi kada se lokacija dobije."
                    )
                    // NE vraćaj se - nastavi sa ostalim filterima
                }

                var filteredObjects = _state.value.allMapObjects
                Log.d(TAG, "   📦 Počinjemo sa ${filteredObjects.size} ukupnih objekata")

                // 1. Filter po pretrazi (naziv ili opis)
                val hasSearchQuery = filter.searchQuery.isNotBlank()
                if (hasSearchQuery) {
                    filteredObjects = filteredObjects.filter { obj ->
                        obj.title.contains(filter.searchQuery, ignoreCase = true) ||
                        obj.description.contains(filter.searchQuery, ignoreCase = true)
                    }
                    Log.d(TAG, "   ✓ Posle pretrage: ${filteredObjects.size} objekata")
                }

                // 2. Filter po tipu objekta (PRVO primeni tip filter)
                if (filter.selectedType != null) {
                    filteredObjects = filteredObjects.filter { obj ->
                        obj.type == filter.selectedType
                    }
                    Log.d(TAG, "   ✓ Posle filtera po tipu: ${filteredObjects.size} objekata")
                }

                // 3. Filter po radijusu (ako je postavljen i ako postoji trenutna lokacija)
                if (filter.radiusInMeters != null) {
                    if (_state.value.currentLocation != null) {
                        val currentLoc = _state.value.currentLocation!!
                        Log.d(TAG, "   📍 Trenutna lokacija: ${currentLoc.latitude}, ${currentLoc.longitude}")
                        Log.d(TAG, "   📏 Filtriram objekte u radijusu od ${filter.radiusInMeters}m")

                        filteredObjects = filteredObjects.filter { obj ->
                            val distance = calculateDistance(
                                currentLoc.latitude, currentLoc.longitude,
                                obj.location.latitude, obj.location.longitude
                            )
                            Log.d(TAG, "      - ${obj.title}: ${distance.toInt()}m (${if (distance <= filter.radiusInMeters) "✅ UKLJUČEN" else "❌ ISKLJUČEN"})")
                            distance <= filter.radiusInMeters
                        }
                        Log.d(TAG, "   ✅ Posle filtera po radijusu: ${filteredObjects.size} objekata")
                    } else {
                        Log.e(TAG, "   ⚠️ PRESKAČEM radijus filter - nema trenutne lokacije!")
                    }
                }

                // 4. Filter po minimalnoj oceni
                if (filter.minRating > 0) {
                    filteredObjects = filteredObjects.filter { obj ->
                        obj.averageRating >= filter.minRating
                    }
                    Log.d(TAG, "   ✓ Posle filtera po oceni: ${filteredObjects.size} objekata")
                }

                // 5. NOVO: Dodaj roditeljske teretane SAMO ako je search query aktivan
                // i SAMO ako te teretane zadovoljavaju sve ostale filtere
                if (hasSearchQuery) {
                    val childObjects = filteredObjects.filter { it.parentGymId != null }
                    val parentGymIds = childObjects.mapNotNull { it.parentGymId }.distinct()

                    if (parentGymIds.isNotEmpty()) {
                        Log.d(TAG, "   🔍 Tražim roditeljske teretane za ${parentGymIds.size} ID-eva...")

                        var parentGyms = _state.value.allMapObjects.filter { gym ->
                            gym.type == veljko.najdanovic19273.fitmap.data.model.ObjectType.GYM &&
                            parentGymIds.contains(gym.id)
                        }

                        Log.d(TAG, "   ✓ Pronađeno ${parentGyms.size} roditeljskih teretana")


                        if (filter.radiusInMeters != null && _state.value.currentLocation != null) {
                            val currentLoc = _state.value.currentLocation!!
                            parentGyms = parentGyms.filter { gym ->
                                val distance = calculateDistance(
                                    currentLoc.latitude, currentLoc.longitude,
                                    gym.location.latitude, gym.location.longitude
                                )
                                distance <= filter.radiusInMeters
                            }
                            Log.d(TAG, "   ✓ Posle radijus filtera: ${parentGyms.size} roditeljskih teretana")
                        }

                        if (filter.minRating > 0) {
                            parentGyms = parentGyms.filter { gym ->
                                gym.averageRating >= filter.minRating
                            }
                            Log.d(TAG, "   ✓ Posle ocena filtera: ${parentGyms.size} roditeljskih teretana")
                        }

                        // Kombinuj pronađene objekte sa njihovim roditeljskim teretanama
                        filteredObjects = (filteredObjects + parentGyms).distinctBy { it.id }
                        Log.d(TAG, "   ✅ Dodato ${parentGyms.size} validnih roditeljskih teretana")
                    }
                }

                Log.d(TAG, "═══════════════════════════════════════")
                Log.d(TAG, "✅ UKUPNO FILTRIRANIH OBJEKATA: ${filteredObjects.size}")
                val gymCount = filteredObjects.filter { it.type == veljko.najdanovic19273.fitmap.data.model.ObjectType.GYM }.size
                val childCount = filteredObjects.filter { it.type != veljko.najdanovic19273.fitmap.data.model.ObjectType.GYM }.size
                Log.d(TAG, "   📍 Teretana: $gymCount")
                Log.d(TAG, "   💪 Child objekata: $childCount")
                Log.d(TAG, "═══════════════════════════════════════")

                _state.value = _state.value.copy(
                    mapObjects = filteredObjects,
                    filterState = filter,
                    error = null // Očisti grešku ako je filter uspešno primenjen
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

    // NOVO: Real-time listener za detalje objekta
    fun observeObjectDetails(
        objectId: String,
        onResult: (MapObject?, List<veljko.najdanovic19273.fitmap.data.model.Comment>, Float?) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration {

        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

        // Listener za objekat
        val objectListener = firestore.collection(Constants.OBJECTS_COLLECTION)
            .document(objectId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Greška pri praćenju objekta: ${error.message}", error)
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) {
                    Log.e(TAG, "❌ Objekat ne postoji")
                    onResult(null, emptyList(), null)
                    return@addSnapshotListener
                }

                val mapObject = MapObject.fromFirestore(snapshot.id, snapshot.data ?: emptyMap())

                if (mapObject == null) {
                    Log.e(TAG, "❌ Greška pri parsiranju objekta")
                    onResult(null, emptyList(), null)
                    return@addSnapshotListener
                }

                Log.d(TAG, "🔄 REAL-TIME UPDATE - Objekat: ${mapObject.title}")

                // Učitaj komentare u realnom vremenu
                firestore.collection(Constants.COMMENTS_COLLECTION)
                    .whereEqualTo("objectId", objectId)
                    .addSnapshotListener { commentsSnapshot, commentsError ->
                        if (commentsError != null) {
                            Log.e(TAG, "❌ Greška pri praćenju komentara: ${commentsError.message}")
                            return@addSnapshotListener
                        }

                        val comments = commentsSnapshot?.documents?.mapNotNull { doc ->
                            doc.toObject(veljko.najdanovic19273.fitmap.data.model.Comment::class.java)?.copy(id = doc.id)
                        }?.sortedByDescending { it.createdAt } ?: emptyList()

                        Log.d(TAG, "🔄 REAL-TIME UPDATE - Komentari: ${comments.size}")

                        // Učitaj korisničku ocenu
                        if (currentUserId != null) {
                            firestore.collection(Constants.RATINGS_COLLECTION)
                                .whereEqualTo("objectId", objectId)
                                .whereEqualTo("authorId", currentUserId)
                                .addSnapshotListener { ratingsSnapshot, ratingsError ->
                                    if (ratingsError != null) {
                                        Log.e(TAG, "❌ Greška pri praćenju ocena")
                                        return@addSnapshotListener
                                    }

                                    val userRating = ratingsSnapshot?.documents?.firstOrNull()
                                        ?.toObject(veljko.najdanovic19273.fitmap.data.model.Rating::class.java)
                                        ?.value

                                    Log.d(TAG, "🔄 REAL-TIME UPDATE - Ocena: ${userRating ?: "nema"}")
                                    onResult(mapObject, comments, userRating)
                                }
                        } else {
                            onResult(mapObject, comments, null)
                        }
                    }
            }

        return objectListener
    }

    // NOVO: Real-time listener za child objekte (sprave u teretani)
    fun observeChildObjects(
        parentGymId: String,
        onResult: (List<MapObject>) -> Unit
    ): com.google.firebase.firestore.ListenerRegistration {
        return firestore.collection(Constants.OBJECTS_COLLECTION)
            .whereEqualTo("parentGymId", parentGymId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Greška pri praćenju child objekata: ${error.message}")
                    onResult(emptyList())
                    return@addSnapshotListener
                }

                val childObjects = snapshot?.documents?.mapNotNull { doc ->
                    MapObject.fromFirestore(doc.id, doc.data ?: emptyMap())
                } ?: emptyList()

                Log.d(TAG, "🔄 REAL-TIME UPDATE - Child objekti: ${childObjects.size}")
                onResult(childObjects)
            }
    }

   //Real-time listener za sve objekte na mapi
    fun observeMapObjects() {
        firestore.collection(Constants.OBJECTS_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "❌ Greška pri praćenju objekata na mapi: ${error.message}")
                    return@addSnapshotListener
                }

                val objects = snapshot?.documents?.mapNotNull { doc ->
                    MapObject.fromFirestore(doc.id, doc.data ?: emptyMap())
                } ?: emptyList()

                Log.d(TAG, "🔄 REAL-TIME UPDATE - Mapa: ${objects.size} objekata")

                _state.value = _state.value.copy(
                    mapObjects = if (_state.value.filterState.isActive()) {
                        // Ako je filter aktivan, primeni ga ponovo
                        applyFilterToObjects(objects, _state.value.filterState)
                    } else {
                        objects
                    },
                    allMapObjects = objects
                )
            }
    }

    // Helper funkcija za primenu filtera na listu objekata
    private fun applyFilterToObjects(
        objects: List<MapObject>,
        filter: veljko.najdanovic19273.fitmap.data.model.FilterState
    ): List<MapObject> {
        var filteredObjects = objects

        // Filter po pretrazi
        val hasSearchQuery = filter.searchQuery.isNotBlank()
        if (hasSearchQuery) {
            filteredObjects = filteredObjects.filter { obj ->
                obj.title.contains(filter.searchQuery, ignoreCase = true) ||
                obj.description.contains(filter.searchQuery, ignoreCase = true)
            }
        }

        // Filter po tipu
        if (filter.selectedType != null) {
            filteredObjects = filteredObjects.filter { obj ->
                obj.type == filter.selectedType
            }
        }

        // Filter po radijusu
        if (filter.radiusInMeters != null && _state.value.currentLocation != null) {
            val currentLoc = _state.value.currentLocation!! // trenutna lokacija korisnika
            filteredObjects = filteredObjects.filter { obj ->
                val distance = calculateDistance(
                    currentLoc.latitude, currentLoc.longitude,
                    obj.location.latitude, obj.location.longitude
                )
                distance <= filter.radiusInMeters
            }
        }

        // Filter po oceni
        if (filter.minRating > 0) {
            filteredObjects = filteredObjects.filter { obj ->
                obj.averageRating >= filter.minRating
            }
        }

        // Dodaj roditeljske teretane
        if (hasSearchQuery) {
            val childObjects = filteredObjects.filter { it.parentGymId != null }
            val parentGymIds = childObjects.mapNotNull { it.parentGymId }.distinct()

            if (parentGymIds.isNotEmpty()) {
                var parentGyms = objects.filter { gym ->
                    gym.type == veljko.najdanovic19273.fitmap.data.model.ObjectType.GYM &&
                    parentGymIds.contains(gym.id)
                }

                // Primeni filtere na roditeljske teretane
                if (filter.radiusInMeters != null && _state.value.currentLocation != null) {
                    val currentLoc = _state.value.currentLocation!!
                    parentGyms = parentGyms.filter { gym ->
                        val distance = calculateDistance(
                            currentLoc.latitude, currentLoc.longitude,
                            gym.location.latitude, gym.location.longitude
                        )
                        distance <= filter.radiusInMeters
                    }
                }

                if (filter.minRating > 0) {
                    parentGyms = parentGyms.filter { gym ->
                        gym.averageRating >= filter.minRating
                    }
                }

                filteredObjects = (filteredObjects + parentGyms).distinctBy { it.id }
            }
        }

        return filteredObjects
    }
}
