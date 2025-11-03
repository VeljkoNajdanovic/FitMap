package veljko.najdanovic19273.fitmap.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import veljko.najdanovic19273.fitmap.data.model.MapObject
import veljko.najdanovic19273.fitmap.data.model.User
import veljko.najdanovic19273.fitmap.util.Constants

class UserRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "UserRepository"
    }

    val currentUserId: String? get() = auth.currentUser?.uid

    // Dobijanje podataka korisnika po ID-ju
    suspend fun getUserById(userId: String): Result<User> {
        return try {
            val snapshot = firestore.collection(Constants.USERS_COLLECTION)
                .document(userId)
                .get()
                .await()

            val user = snapshot.toObject(User::class.java)
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Korisnik nije pronađen"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Greška pri dobijanju korisnika", e)
            Result.failure(e)
        }
    }

    // Dobijanje trenutnog korisnika
    suspend fun getCurrentUser(): Result<User> {
        return try {
            val userId = currentUserId ?: throw Exception("Korisnik nije prijavljen")
            getUserById(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Greška pri dobijanju trenutnog korisnika", e)
            Result.failure(e)
        }
    }

    // Ažuriranje profila korisnika
    suspend fun updateProfile(
        firstName: String? = null,
        lastName: String? = null,
        username: String? = null,
        phoneNumber: String? = null,
        profileImageUrl: String? = null
    ): Result<Unit> {
        return try {
            val userId = currentUserId ?: throw Exception("Korisnik nije prijavljen")
            val updates = mutableMapOf<String, Any>()

            firstName?.let { updates["firstName"] = it }
            lastName?.let { updates["lastName"] = it }
            username?.let { updates["username"] = it }
            phoneNumber?.let { updates["phoneNumber"] = it }
            profileImageUrl?.let { updates["profileImageUrl"] = it }

            if (updates.isNotEmpty()) {
                firestore.collection(Constants.USERS_COLLECTION)
                    .document(userId)
                    .update(updates)
                    .await()
                Log.d(TAG, "Profil uspešno ažuriran")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Greška pri ažuriranju profila", e)
            Result.failure(e)
        }
    }

    // Dobijanje objekata koje je korisnik kreirao
    suspend fun getUserObjects(userId: String): Result<List<MapObject>> {
        return try {
            // Učitaj bez orderBy da izbegnemo potrebu za indeksom
            val snapshot = firestore.collection(Constants.OBJECTS_COLLECTION)
                .whereEqualTo("authorId", userId)
                .get()
                .await()

            // Sortiraj na klijentskoj strani
            val objects = snapshot.documents.mapNotNull { doc ->
                MapObject.fromFirestore(doc.id, doc.data ?: emptyMap())
            }.sortedByDescending { it.createdAt } // Sortiraj po datumu kreiranja

            Log.d(TAG, "Pronađeno ${objects.size} objekata za korisnika $userId")
            Result.success(objects)
        } catch (e: Exception) {
            Log.e(TAG, "Greška pri dobijanju objekata korisnika", e)
            Result.failure(e)
        }
    }

    // Dodavanje poena korisniku
    suspend fun addPoints(userId: String, points: Int): Result<Unit> {
        return try {
            val userRef = firestore.collection(Constants.USERS_COLLECTION).document(userId)

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val currentPoints = snapshot.getLong("points")?.toInt() ?: 0
                transaction.update(userRef, "points", currentPoints + points)
            }.await()

            Log.d(TAG, "Dodato $points poena korisniku $userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Greška pri dodavanju poena", e)
            Result.failure(e)
        }
    }

    // Dobijanje top korisnika (za leaderboard)
    suspend fun getTopUsers(limit: Int = 10): Result<List<User>> {
        return try {
            // Učitaj sve korisnike bez orderBy
            val snapshot = firestore.collection(Constants.USERS_COLLECTION)
                .get()
                .await()

            // Sortiraj na klijentskoj strani i uzmi top N
            val users = snapshot.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)?.copy(id = doc.id)
            }.sortedByDescending { it.points } // Sortiraj po poenima
              .take(limit) // Uzmi top N korisnika

            Log.d(TAG, "Pronađeno ${users.size} top korisnika")
            Result.success(users)
        } catch (e: Exception) {
            Log.e(TAG, "Greška pri dobijanju top korisnika", e)
            Result.failure(e)
        }
    }

    // Dobijanje pozicije korisnika na rang listi
    suspend fun getUserRank(userId: String): Result<Int> {
        return try {
            val userResult = getUserById(userId)
            if (userResult.isFailure) {
                return Result.failure(userResult.exceptionOrNull() ?: Exception("Greška"))
            }

            val user = userResult.getOrNull()!!
            val userPoints = user.points

            // Prebrojavanje koliko korisnika ima više poena
            val snapshot = firestore.collection(Constants.USERS_COLLECTION)
                .whereGreaterThan("points", userPoints)
                .get()
                .await()

            val rank = snapshot.size() + 1
            Log.d(TAG, "Korisnik $userId je na poziciji $rank")
            Result.success(rank)
        } catch (e: Exception) {
            Log.e(TAG, "Greška pri dobijanju ranga korisnika", e)
            Result.failure(e)
        }
    }
}
