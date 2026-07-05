package veljko.najdanovic19273.fitmap.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import veljko.najdanovic19273.fitmap.data.model.Comment
import veljko.najdanovic19273.fitmap.data.model.MapObject
import veljko.najdanovic19273.fitmap.data.model.ObjectType
import veljko.najdanovic19273.fitmap.data.model.Rating
import veljko.najdanovic19273.fitmap.util.Constants

class MapObjectRepository {
    private val firestore = FirebaseFirestore.getInstance()

    // Dodavanje novog objekta
    suspend fun addObject(mapObject: MapObject): Result<String> {
        return try {
            val docRef = firestore.collection(Constants.OBJECTS_COLLECTION)
                .add(mapObject.toMap())
                .await()

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Dobijanje svih objekata (real-time)
    fun getAllObjects(): Flow<List<MapObject>> = callbackFlow {
        val subscription = firestore.collection(Constants.OBJECTS_COLLECTION)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val objects = snapshot?.documents?.mapNotNull {
                    it.toObject(MapObject::class.java)?.copy(id = it.id)
                } ?: emptyList()

                trySend(objects)
            }

        awaitClose { subscription.remove() }
    }

    // Pretraga objekata po tipu
    fun getObjectsByType(type: ObjectType): Flow<List<MapObject>> = callbackFlow {
        val subscription = firestore.collection(Constants.OBJECTS_COLLECTION)
            .whereEqualTo("type", type.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val objects = snapshot?.documents?.mapNotNull {
                    it.toObject(MapObject::class.java)?.copy(id = it.id)
                } ?: emptyList()

                trySend(objects)
            }

        awaitClose { subscription.remove() }
    }

    // Pretraga objekata po autoru
    fun getObjectsByAuthor(authorId: String): Flow<List<MapObject>> = callbackFlow {
        val subscription = firestore.collection(Constants.OBJECTS_COLLECTION)
            .whereEqualTo("authorId", authorId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val objects = snapshot?.documents?.mapNotNull {
                    it.toObject(MapObject::class.java)?.copy(id = it.id)
                } ?: emptyList()

                trySend(objects)
            }

        awaitClose { subscription.remove() }
    }

    // Dodavanje komentara
    suspend fun addComment(comment: Comment): Result<String> {
        return try {
            val docRef = firestore.collection(Constants.COMMENTS_COLLECTION)
                .add(comment.toMap())
                .await()

            // Ažuriranje broja komentara
            firestore.collection(Constants.OBJECTS_COLLECTION)
                .document(comment.objectId)
                .update("commentsCount", FieldValue.increment(1))
                .await()

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Dobijanje komentara za objekat
    fun getComments(objectId: String): Flow<List<Comment>> = callbackFlow {
        val subscription = firestore.collection(Constants.COMMENTS_COLLECTION)
            .whereEqualTo("objectId", objectId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val comments = snapshot?.documents?.mapNotNull {
                    it.toObject(Comment::class.java)?.copy(id = it.id)
                } ?: emptyList()

                trySend(comments)
            }

        awaitClose { subscription.remove() }
    }

    // Dodavanje ocene
    suspend fun addRating(rating: Rating): Result<Unit> {
        return try {
            // Provera da li korisnik već ima ocenu za ovaj objekat
            val existingRating = firestore.collection(Constants.RATINGS_COLLECTION)
                .whereEqualTo("objectId", rating.objectId)
                .whereEqualTo("authorId", rating.authorId)
                .get()
                .await()

            if (!existingRating.isEmpty) {
                // Ažuriranje postojeće ocene
                val ratingId = existingRating.documents[0].id
                firestore.collection(Constants.RATINGS_COLLECTION)
                    .document(ratingId)
                    .update("value", rating.value)
                    .await()
            } else {
                // Dodavanje nove ocene
                firestore.collection(Constants.RATINGS_COLLECTION)
                    .add(rating.toMap())
                    .await()

                // Povećanje broja ocena
                firestore.collection(Constants.OBJECTS_COLLECTION)
                    .document(rating.objectId)
                    .update("ratingsCount", FieldValue.increment(1))
                    .await()
            }

            // Ažuriranje prosečne ocene
            updateAverageRating(rating.objectId)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Ažuriranje prosečne ocene objekta
    private suspend fun updateAverageRating(objectId: String) {
        val ratings = firestore.collection(Constants.RATINGS_COLLECTION)
            .whereEqualTo("objectId", objectId)
            .get()
            .await()

        val ratingsList = ratings.documents.mapNotNull {
            it.toObject(Rating::class.java)
        }

        if (ratingsList.isNotEmpty()) {
            val average = ratingsList.map { it.value }.average()
            firestore.collection(Constants.OBJECTS_COLLECTION)
                .document(objectId)
                .update("averageRating", average)
                .await()
        }
    }

    // Dobijanje ocene korisnika za objekat
    suspend fun getUserRating(objectId: String, userId: String): Result<Rating?> {
        return try {
            val snapshot = firestore.collection(Constants.RATINGS_COLLECTION)
                .whereEqualTo("objectId", objectId)
                .whereEqualTo("authorId", userId)
                .get()
                .await()

            val rating = if (!snapshot.isEmpty) {
                snapshot.documents[0].toObject(Rating::class.java)?.copy(
                    id = snapshot.documents[0].id
                )
            } else null

            Result.success(rating)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

