package veljko.najdanovic19273.fitmap.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import veljko.najdanovic19273.fitmap.data.model.User
import veljko.najdanovic19273.fitmap.util.Constants

class AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "AuthRepository"
    }

    val currentUser: FirebaseUser? get() = auth.currentUser

    // Registracija korisnika: kreira FirebaseAuth nalog i upisuje korisnika u Firestore
    suspend fun register(
        email: String,
        password: String,
        username: String,
        firstName: String,
        lastName: String,
        phoneNumber: String,
        profileImageUrl: String = ""
    ): Result<User> {
        return try {
            Log.d(TAG, "Pokušaj registracije: email=$email, username=$username")

            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: throw Exception("User ID je null")

            Log.d(TAG, "Firebase Auth uspešan, userId=$userId, kreiran Firestore dokument...")

            val user = User(
                id = userId,
                email = email,
                username = username,
                firstName = firstName,
                lastName = lastName,
                phoneNumber = phoneNumber,
                profileImageUrl = profileImageUrl,
                points = 0
            )

            firestore.collection(Constants.USERS_COLLECTION)
                .document(userId)
                .set(user.toMap())
                .await()

            Log.d(TAG, "Firestore dokument uspešno kreiran!")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Greška pri registraciji", e)
            Result.failure(e)
        }
    }

    // Prijava korisnika
    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            Log.d(TAG, "Pokušaj prijave: email=$email")
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            Log.d(TAG, "Prijava uspešna: ${authResult.user?.email}")
            Result.success(authResult.user!!)
        } catch (e: Exception) {
            Log.e(TAG, "Greška pri prijavi", e)
            Result.failure(e)
        }
    }

    // Odjava
    fun logout() {
        auth.signOut()
    }

    // Dobijanje podataka trenutnog korisnika iz Firestore
    suspend fun getCurrentUserData(): Result<User> {
        return try {
            val userId = currentUser?.uid ?: throw Exception("Korisnik nije prijavljen")
            val snapshot = firestore.collection(Constants.USERS_COLLECTION)
                .document(userId)
                .get()
                .await()

            val user = snapshot.toObject(User::class.java) ?: throw Exception("Korisnik ne postoji")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Ažuriranje FCM tokena
    suspend fun updateFcmToken(token: String): Result<Unit> {
        return try {
            val userId = currentUser?.uid ?: throw Exception("Korisnik nije prijavljen")
            firestore.collection(Constants.USERS_COLLECTION)
                .document(userId)
                .update("fcmToken", token)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Ažuriranje profila
    suspend fun updateProfile(
        username: String? = null,
        fullName: String? = null,
        phoneNumber: String? = null,
        profileImageUrl: String? = null
    ): Result<Unit> {
        return try {
            val userId = currentUser?.uid ?: throw Exception("Korisnik nije prijavljen")
            val updates = mutableMapOf<String, Any>()

            username?.let { updates["username"] = it }
            fullName?.let { updates["fullName"] = it }
            phoneNumber?.let { updates["phoneNumber"] = it }
            profileImageUrl?.let { updates["profileImageUrl"] = it }

            if (updates.isNotEmpty()) {
                firestore.collection(Constants.USERS_COLLECTION)
                    .document(userId)
                    .update(updates)
                    .await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
